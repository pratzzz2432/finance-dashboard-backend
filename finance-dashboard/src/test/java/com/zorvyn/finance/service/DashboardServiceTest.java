package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.enums.RecordType;
import com.zorvyn.finance.dto.response.DashboardSummary;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private FinancialRecordRepository recordRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getSummary_calculatesNetBalanceCorrectly() {
        when(recordRepository.sumByType(RecordType.INCOME)).thenReturn(new BigDecimal("10000.00"));
        when(recordRepository.sumByType(RecordType.EXPENSE)).thenReturn(new BigDecimal("3500.00"));
        when(recordRepository.sumByCategoryAndType(any())).thenReturn(new ArrayList<>());
        when(recordRepository.findRecentRecords(any())).thenReturn(Collections.emptyList());
        when(recordRepository.monthlyTrends(any(), any())).thenReturn(new ArrayList<>());

        DashboardSummary summary = dashboardService.getSummary();

        assertNotNull(summary);
        assertEquals(new BigDecimal("10000.00"), summary.getTotalIncome());
        assertEquals(new BigDecimal("3500.00"), summary.getTotalExpenses());
        assertEquals(new BigDecimal("6500.00"), summary.getNetBalance());
    }

    @Test
    void getSummary_zeroRecords_returnsZeroBalances() {
        when(recordRepository.sumByType(RecordType.INCOME)).thenReturn(BigDecimal.ZERO);
        when(recordRepository.sumByType(RecordType.EXPENSE)).thenReturn(BigDecimal.ZERO);
        when(recordRepository.sumByCategoryAndType(any())).thenReturn(new ArrayList<>());
        when(recordRepository.findRecentRecords(any())).thenReturn(Collections.emptyList());
        when(recordRepository.monthlyTrends(any(), any())).thenReturn(new ArrayList<>());

        DashboardSummary summary = dashboardService.getSummary();

        assertEquals(BigDecimal.ZERO, summary.getNetBalance());
    }
}
