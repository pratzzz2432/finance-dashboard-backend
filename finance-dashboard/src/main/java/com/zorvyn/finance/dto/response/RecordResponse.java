package com.zorvyn.finance.dto.response;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class RecordResponse {

    private Long id;
    private BigDecimal amount;
    private String type;
    private String category;
    private LocalDate date;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RecordResponse fromEntity(FinancialRecord record) {
        RecordResponse response = new RecordResponse();
        response.setId(record.getId());
        response.setAmount(record.getAmount());
        response.setType(record.getType().name());
        response.setCategory(record.getCategory());
        response.setDate(record.getDate());
        response.setDescription(record.getDescription());
        response.setCreatedBy(record.getCreatedBy().getUsername());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        return response;
    }
}
