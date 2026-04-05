package com.zorvyn.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrend {

    private int year;
    private int month;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal net;
}
