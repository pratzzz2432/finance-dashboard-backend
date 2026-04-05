package com.zorvyn.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateRecordRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String type;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    private LocalDate date;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
