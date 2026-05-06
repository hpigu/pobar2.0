package com.pobar.service.impl;

import com.pobar.dto.report.DailyRevenueResponse;
import com.pobar.dto.report.MonthlyComparisonResponse;
import com.pobar.dto.report.SalesRankingResponse;
import com.pobar.mapper.OrderItemMapper;
import com.pobar.mapper.PaymentMapper;
import com.pobar.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    // 業務日從 04:00 開始
    private static final LocalTime BUSINESS_DAY_START = LocalTime.of(4, 0);

    private final PaymentMapper paymentMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public DailyRevenueResponse dailyRevenue(LocalDate date) {
        LocalDateTime from = date.atTime(BUSINESS_DAY_START);
        LocalDateTime to = date.plusDays(1).atTime(BUSINESS_DAY_START).minusNanos(1);

        Map<String, Object> summary = paymentMapper.selectDailySummary(from, to);
        List<Map<String, Object>> hourlyRaw = paymentMapper.selectHourlyRevenue(from, to);

        // 填入 0~23 完整小時資料
        Map<Integer, Map<String, Object>> hourMap = hourlyRaw.stream()
                .collect(Collectors.toMap(m -> toInt(m.get("hour")), m -> m));

        List<DailyRevenueResponse.HourlyData> hourly = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            DailyRevenueResponse.HourlyData hd = new DailyRevenueResponse.HourlyData();
            hd.setHour(h);
            if (hourMap.containsKey(h)) {
                Map<String, Object> m = hourMap.get(h);
                hd.setRevenue(toBigDecimal(m.get("revenue")));
                hd.setOrders(toInt(m.get("orders")));
            } else {
                hd.setRevenue(BigDecimal.ZERO);
                hd.setOrders(0);
            }
            hourly.add(hd);
        }

        DailyRevenueResponse resp = new DailyRevenueResponse();
        resp.setDate(date);
        resp.setTotalRevenue(toBigDecimal(summary.get("totalRevenue")));
        resp.setTotalServiceCharge(toBigDecimal(summary.get("totalServiceCharge")));
        resp.setOrderCount(toInt(summary.get("orderCount")));
        resp.setGuestCount(toInt(summary.get("guestCount")));
        resp.setHourly(hourly);
        return resp;
    }

    @Override
    public SalesRankingResponse salesRanking(LocalDate from, LocalDate to, int limit) {
        LocalDateTime dtFrom = from.atTime(BUSINESS_DAY_START);
        LocalDateTime dtTo = to.plusDays(1).atTime(BUSINESS_DAY_START).minusNanos(1);

        List<Map<String, Object>> raw = orderItemMapper.selectSalesRanking(dtFrom, dtTo, limit);

        List<SalesRankingResponse.ProductRank> ranks = raw.stream().map(m -> {
            SalesRankingResponse.ProductRank rank = new SalesRankingResponse.ProductRank();
            rank.setProductId(toInt(m.get("productId")));
            rank.setProductName((String) m.get("productName"));
            rank.setCategoryName((String) m.get("categoryName"));
            rank.setTotalQuantity(toInt(m.get("totalQuantity")));
            rank.setTotalRevenue(toBigDecimal(m.get("totalRevenue")));
            return rank;
        }).toList();

        SalesRankingResponse resp = new SalesRankingResponse();
        resp.setProducts(ranks);
        return resp;
    }

    @Override
    public MonthlyComparisonResponse monthlyComparison(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime from = ym.atDay(1).atTime(BUSINESS_DAY_START);
        LocalDateTime to = ym.atEndOfMonth().plusDays(1).atTime(BUSINESS_DAY_START).minusNanos(1);

        // 同期去年
        YearMonth lastYm = ym.minusYears(1);
        LocalDateTime lastFrom = lastYm.atDay(1).atTime(BUSINESS_DAY_START);
        LocalDateTime lastTo = lastYm.atEndOfMonth().plusDays(1).atTime(BUSINESS_DAY_START).minusNanos(1);

        List<Map<String, Object>> current = paymentMapper.selectDailyRevenueSeries(from, to);
        List<Map<String, Object>> last = paymentMapper.selectDailyRevenueSeries(lastFrom, lastTo);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        // 以當年日期為 x 軸
        Map<String, BigDecimal> currentMap = current.stream()
                .collect(Collectors.toMap(m -> m.get("date").toString(), m -> toBigDecimal(m.get("revenue"))));
        Map<String, BigDecimal> lastMap = last.stream()
                .collect(Collectors.toMap(m -> m.get("date").toString(), m -> toBigDecimal(m.get("revenue"))));

        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<BigDecimal> lastYear = new ArrayList<>();

        LocalDate cursor = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        while (!cursor.isAfter(end)) {
            String key = cursor.toString();                // yyyy-MM-dd
            String lastKey = cursor.minusYears(1).toString();
            String label = cursor.format(fmt);

            dates.add(label);
            revenues.add(currentMap.getOrDefault(key, BigDecimal.ZERO));
            lastYear.add(lastMap.getOrDefault(lastKey, BigDecimal.ZERO));
            cursor = cursor.plusDays(1);
        }

        MonthlyComparisonResponse resp = new MonthlyComparisonResponse();
        resp.setDates(dates);
        resp.setRevenues(revenues);
        resp.setLastYear(lastYear);
        return resp;
    }

    // ── 型別轉換工具 ──────────────────────────────────────────────────────────

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number n) return n.intValue();
        return Integer.parseInt(val.toString());
    }
}
