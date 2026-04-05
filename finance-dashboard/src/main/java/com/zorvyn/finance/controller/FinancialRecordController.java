package com.zorvyn.finance.controller;

import com.zorvyn.finance.domain.enums.RecordType;
import com.zorvyn.finance.dto.request.CreateRecordRequest;
import com.zorvyn.finance.dto.request.UpdateRecordRequest;
import com.zorvyn.finance.dto.response.RecordResponse;
import com.zorvyn.finance.exception.BadRequestException;
import com.zorvyn.finance.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
@Tag(name = "Financial Records", description = "Endpoints for managing financial transactions")
public class FinancialRecordController {

    @Autowired
    private FinancialRecordService recordService;

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "Get all records with optional filters and pagination")
    public ResponseEntity<Page<RecordResponse>> getRecords(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "date") Pageable pageable) {

        RecordType recordType = null;
        if (type != null) {
            try {
                recordType = RecordType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid type filter: " + type);
            }
        }

        return ResponseEntity.ok(recordService.getRecords(recordType, category, startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "Get a specific record by ID")
    public ResponseEntity<RecordResponse> getRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new financial record (Admin only)")
    public ResponseEntity<RecordResponse> createRecord(@Valid @RequestBody CreateRecordRequest request) {
        RecordResponse response = recordService.createRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing record (Admin only)")
    public ResponseEntity<RecordResponse> updateRecord(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateRecordRequest request) {
        return ResponseEntity.ok(recordService.updateRecord(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a financial record (Admin only)")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        recordService.softDeleteRecord(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Operation(summary = "Get all available record categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(recordService.getCategories());
    }
}
