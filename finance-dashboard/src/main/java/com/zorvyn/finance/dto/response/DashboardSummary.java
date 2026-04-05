package com.zorvyn.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private List<CategoryBreakdown> incomeByCategory;
    private List<CategoryBreakdown> expenseByCategory;
    private List<RecordResponse> recentActivity;
    private List<MonthlyTrend> monthlyTrends;
}
