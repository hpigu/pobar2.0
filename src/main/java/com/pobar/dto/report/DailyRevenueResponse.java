package com.pobar.dto.report;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DailyRevenueResponse {

    private LocalDate date;
    private BigDecimal totalRevenue;
    private BigDecimal totalServiceCharge;
    private Integer orderCount;
    private Integer guestCount;

    // ECharts 用：當日每小時收入（0~23）
    private List<HourlyData> hourly;

    @Data
    public static class HourlyData {
        private Integer hour;
        private BigDecimal revenue;
        private Integer orders;
    }
}
