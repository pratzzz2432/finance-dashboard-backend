package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RecordType;
import com.zorvyn.finance.dto.request.CreateRecordRequest;
import com.zorvyn.finance.dto.request.UpdateRecordRequest;
import com.zorvyn.finance.dto.response.RecordResponse;
import com.zorvyn.finance.exception.BadRequestException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FinancialRecordService {

    @Autowired
    private FinancialRecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;

    public Page<RecordResponse> getRecords(RecordType type, String category,
                                           LocalDate startDate, LocalDate endDate,
                                           Pageable pageable) {
        return recordRepository.findWithFilters(type, category, startDate, endDate, pageable)
                .map(RecordResponse::fromEntity);
    }

    public RecordResponse getRecordById(Long id) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record", "id", id));
        return RecordResponse.fromEntity(record);
    }

    @Transactional
    public RecordResponse createRecord(CreateRecordRequest request) {
        RecordType type = parseRecordType(request.getType());

        User currentUser = getCurrentUser();

        FinancialRecord record = new FinancialRecord();
        record.setAmount(request.getAmount());
        record.setType(type);
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setDescription(request.getDescription());
        record.setCreatedBy(currentUser);
        record.setDeleted(false);

        FinancialRecord savedRecord = recordRepository.save(record);
        return RecordResponse.fromEntity(savedRecord);
    }

    // partial update — only fields that are sent get changed
    @Transactional
    public RecordResponse updateRecord(Long id, UpdateRecordRequest request) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record", "id", id));

        if (request.getAmount() != null) {
            record.setAmount(request.getAmount());
        }
        if (request.getType() != null) {
            record.setType(parseRecordType(request.getType()));
        }
        if (request.getCategory() != null) {
            record.setCategory(request.getCategory());
        }
        if (request.getDate() != null) {
            record.setDate(request.getDate());
        }
        if (request.getDescription() != null) {
            record.setDescription(request.getDescription());
        }

        FinancialRecord savedRecord = recordRepository.save(record);
        return RecordResponse.fromEntity(savedRecord);
    }

    // soft delete — mark as deleted instead of removing from DB
    // financial data shouldn't just vanish
    @Transactional
    public void softDeleteRecord(Long id) {
        FinancialRecord record = recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record", "id", id));
        record.setDeleted(true);
        recordRepository.save(record);
    }

    public List<String> getCategories() {
        return recordRepository.findDistinctCategories();
    }

    private RecordType parseRecordType(String type) {
        try {
            return RecordType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid record type: " + type
                    + ". Allowed values: INCOME, EXPENSE");
        }
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
}
