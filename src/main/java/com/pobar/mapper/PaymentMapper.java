package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("SELECT * FROM payment WHERE session_id = #{sessionId}")
    Payment selectBySessionId(Integer sessionId);

    // 每日收入摘要（總額、服務費、桌數、人數）
    @Select("""
            SELECT
              COALESCE(SUM(p.total), 0)           AS totalRevenue,
              COALESCE(SUM(p.service_charge), 0)  AS totalServiceCharge,
              COUNT(p.id)                          AS orderCount,
              COALESCE(SUM(ts.party_size), 0)     AS guestCount
            FROM payment p
            INNER JOIN table_session ts ON ts.id = p.session_id
            WHERE p.paid_at BETWEEN #{from} AND #{to}
            """)
    Map<String, Object> selectDailySummary(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // 每小時收入（0~23）
    @Select("""
            SELECT
              HOUR(paid_at)         AS hour,
              SUM(total)            AS revenue,
              COUNT(id)             AS orders
            FROM payment
            WHERE paid_at BETWEEN #{from} AND #{to}
            GROUP BY HOUR(paid_at)
            ORDER BY hour
            """)
    List<Map<String, Object>> selectHourlyRevenue(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    // 指定區間每日收入（用於月趨勢圖）
    @Select("""
            SELECT
              DATE(paid_at)   AS date,
              SUM(total)      AS revenue
            FROM payment
            WHERE paid_at BETWEEN #{from} AND #{to}
            GROUP BY DATE(paid_at)
            ORDER BY date
            """)
    List<Map<String, Object>> selectDailyRevenueSeries(@Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to);
}
