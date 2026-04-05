package com.zorvyn.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateRecordRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Type is required (INCOME or EXPENSE)")
    private String type;

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
