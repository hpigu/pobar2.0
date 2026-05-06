package com.pobar.dto.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthlyComparisonResponse {

    // ECharts 折線圖：近 N 個月的每日收入
    private List<String> dates;          // x 軸日期，格式 "MM-dd"
    private List<BigDecimal> revenues;   // y 軸收入
    private List<BigDecimal> lastYear;   // 去年同期（可選）
}
