package com.pobar.service;

import com.pobar.dto.report.DailyRevenueResponse;
import com.pobar.dto.report.MonthlyComparisonResponse;
import com.pobar.dto.report.SalesRankingResponse;

import java.time.LocalDate;

public interface ReportService {

    DailyRevenueResponse dailyRevenue(LocalDate date);

    SalesRankingResponse salesRanking(LocalDate from, LocalDate to, int limit);

    MonthlyComparisonResponse monthlyComparison(int year, int month);
}
