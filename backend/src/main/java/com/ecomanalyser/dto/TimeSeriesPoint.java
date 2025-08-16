package com.ecomanalyser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class TimeSeriesPoint {
    private LocalDate period;
    private BigDecimal value;
}


