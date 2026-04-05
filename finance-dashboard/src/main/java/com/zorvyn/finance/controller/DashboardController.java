package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.response.DashboardSummary;
import com.zorvyn.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Endpoints for dashboard analytics and summaries")
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get overall dashboard summary with totals, breakdowns, trends")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/summary/range")
    @Operation(summary = "Get dashboard summary for a specific date range")
    public ResponseEntity<DashboardSummary> getSummaryByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(dashboardService.getSummaryForDateRange(startDate, endDate));
    }
}
