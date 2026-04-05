package com.zorvyn.finance.service;

import com.zorvyn.finance.domain.enums.RecordType;
import com.zorvyn.finance.dto.response.*;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private FinancialRecordRepository recordRepository;

    /**
     * Builds the full dashboard snapshot — totals, breakdowns, trends, recent activity.
     * All aggregation happens at the DB level via JPQL to keep things efficient.
     * TODO: could add caching here if the dataset grows large
     */
    public DashboardSummary getSummary() {
        BigDecimal totalIncome = recordRepository.sumByType(RecordType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(RecordType.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        List<CategoryBreakdown> incomeByCategory = buildCategoryBreakdown(RecordType.INCOME);
        List<CategoryBreakdown> expenseByCategory = buildCategoryBreakdown(RecordType.EXPENSE);

        // last 10 transactions for the "recent activity" widget
        List<RecordResponse> recentActivity = recordRepository
                .findRecentRecords(PageRequest.of(0, 10))
                .stream()
                .map(RecordResponse::fromEntity)
                .collect(Collectors.toList());

        // 6-month rolling window for trends — simple default, the /range endpoint allows custom dates
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);
        List<MonthlyTrend> monthlyTrends = buildMonthlyTrends(startDate, endDate);

        return new DashboardSummary(
                totalIncome, totalExpenses, netBalance,
                incomeByCategory, expenseByCategory,
                recentActivity, monthlyTrends
        );
    }

    public DashboardSummary getSummaryForDateRange(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalIncome = recordRepository.sumByTypeAndDateBetween(RecordType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = recordRepository.sumByTypeAndDateBetween(RecordType.EXPENSE, startDate, endDate);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        List<CategoryBreakdown> incomeByCategory = buildCategoryBreakdown(RecordType.INCOME);
        List<CategoryBreakdown> expenseByCategory = buildCategoryBreakdown(RecordType.EXPENSE);

        List<RecordResponse> recentActivity = recordRepository
                .findRecentRecords(PageRequest.of(0, 10))
                .stream()
                .map(RecordResponse::fromEntity)
                .collect(Collectors.toList());

        List<MonthlyTrend> monthlyTrends = buildMonthlyTrends(startDate, endDate);

        return new DashboardSummary(
                totalIncome, totalExpenses, netBalance,
                incomeByCategory, expenseByCategory,
                recentActivity, monthlyTrends
        );
    }

    private List<CategoryBreakdown> buildCategoryBreakdown(RecordType type) {
        return recordRepository.sumByCategoryAndType(type)
                .stream()
                .map(row -> new CategoryBreakdown((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    // assemble monthly income/expense from raw DB rows into structured trend objects
    private List<MonthlyTrend> buildMonthlyTrends(LocalDate startDate, LocalDate endDate) {
        List<Object[]> rawTrends = recordRepository.monthlyTrends(startDate, endDate);

        // group by year-month, then merge income + expense rows into one MonthlyTrend
        Map<String, MonthlyTrend> trendMap = new HashMap<>();

        for (Object[] row : rawTrends) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            RecordType type = (row[2] instanceof RecordType)
                    ? (RecordType) row[2]
                    : RecordType.valueOf(row[2].toString());
            BigDecimal amount = (BigDecimal) row[3];

            String key = year + "-" + month;
            MonthlyTrend trend = trendMap.computeIfAbsent(key,
                    k -> new MonthlyTrend(year, month, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

            if (type == RecordType.INCOME) {
                trend.setIncome(amount);
            } else {
                trend.setExpense(amount);
            }
            trend.setNet(trend.getIncome().subtract(trend.getExpense()));
        }

        List<MonthlyTrend> trends = new ArrayList<>(trendMap.values());
        trends.sort((a, b) -> {
            int yearCompare = Integer.compare(a.getYear(), b.getYear());
            return yearCompare != 0 ? yearCompare : Integer.compare(a.getMonth(), b.getMonth());
        });

        return trends;
    }
}
