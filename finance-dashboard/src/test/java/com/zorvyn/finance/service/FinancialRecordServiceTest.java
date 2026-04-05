package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.entity.Role;
import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RecordType;
import com.zorvyn.finance.domain.enums.RoleType;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.dto.request.CreateRecordRequest;
import com.zorvyn.finance.dto.response.RecordResponse;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialRecordServiceTest {

    @Mock
    private FinancialRecordRepository recordRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FinancialRecordService recordService;

    private User testUser;
    private FinancialRecord sampleRecord;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role(1L, RoleType.ADMIN);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setEmail("admin@test.com");
        testUser.setRole(adminRole);
        testUser.setStatus(UserStatus.ACTIVE);

        sampleRecord = new FinancialRecord();
        sampleRecord.setId(1L);
        sampleRecord.setAmount(new BigDecimal("500.00"));
        sampleRecord.setType(RecordType.INCOME);
        sampleRecord.setCategory("Salary");
        sampleRecord.setDate(LocalDate.of(2026, 3, 1));
        sampleRecord.setDescription("Test income");
        sampleRecord.setCreatedBy(testUser);
        sampleRecord.setDeleted(false);
        sampleRecord.setCreatedAt(LocalDateTime.now());
        sampleRecord.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getRecordById_existingRecord_returnsResponse() {
        when(recordRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));

        RecordResponse response = recordService.getRecordById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("INCOME", response.getType());
        assertEquals("Salary", response.getCategory());
    }

    @Test
    void getRecordById_nonExistingRecord_throwsException() {
        when(recordRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recordService.getRecordById(99L));
    }

    @Test
    void createRecord_validInput_createsSuccessfully() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(recordRepository.save(any(FinancialRecord.class))).thenAnswer(invocation -> {
            FinancialRecord record = invocation.getArgument(0);
            record.setId(10L);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            return record;
        });

        CreateRecordRequest request = new CreateRecordRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setType("INCOME");
        request.setCategory("Bonus");
        request.setDate(LocalDate.of(2026, 4, 1));
        request.setDescription("Quarterly bonus");

        RecordResponse response = recordService.createRecord(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), response.getAmount());
        assertEquals("INCOME", response.getType());
        verify(recordRepository, times(1)).save(any(FinancialRecord.class));
    }

    @Test
    void softDeleteRecord_existingRecord_setsDeletedFlag() {
        when(recordRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any(FinancialRecord.class))).thenReturn(sampleRecord);

        recordService.softDeleteRecord(1L);

        assertTrue(sampleRecord.isDeleted());
        verify(recordRepository, times(1)).save(sampleRecord);
    }
}
