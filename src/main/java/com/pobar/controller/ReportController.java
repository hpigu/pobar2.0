package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.report.DailyRevenueResponse;
import com.pobar.dto.report.MonthlyComparisonResponse;
import com.pobar.dto.report.SalesRankingResponse;
import com.pobar.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily")
    public Result<DailyRevenueResponse> daily(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reportService.dailyRevenue(date != null ? date : LocalDate.now()));
    }

    @GetMapping("/ranking")
    public Result<SalesRankingResponse> ranking(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "20") int limit) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        return Result.ok(reportService.salesRanking(start, end, Math.min(limit, 50)));
    }

    @GetMapping("/monthly")
    public Result<MonthlyComparisonResponse> monthly(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        LocalDate now = LocalDate.now();
        int y = year > 0 ? year : now.getYear();
        int m = month > 0 ? month : now.getMonthValue();
        return Result.ok(reportService.monthlyComparison(y, m));
    }
}
