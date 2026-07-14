package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    // 自動取消：超過寬限時間仍未到場的訂位標為 AUTO_CANCELLED
    @Update("""
            UPDATE reservation
            SET status = 'AUTO_CANCELLED', cancelled_at = NOW()
            WHERE status = 'CONFIRMED'
              AND reserved_at < #{cutoff}
            """)
    int markNoShow(@Param("cutoff") LocalDateTime cutoff);

    // 列出某日期區間的訂位（後台行事曆用）
    @Select("""
            SELECT * FROM reservation
            WHERE reserved_at BETWEEN #{from} AND #{to}
            ORDER BY reserved_at
            """)
    List<Reservation> selectByDateRange(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    // 列出某區間內仍佔用座位的訂位（容量計算用）
    // 只有 CONFIRMED / SEATED 佔容量；CANCELLED / AUTO_CANCELLED / NO_SHOW / COMPLETED 都不算
    @Select("""
            SELECT * FROM reservation
            WHERE reserved_at BETWEEN #{from} AND #{to}
              AND status IN ('CONFIRMED', 'SEATED')
            """)
    List<Reservation> selectActiveByDateRange(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    // 顧客以手機 + 訂位代碼查自己的訂位（近 90 天，最新在前）
    @Select("""
            SELECT * FROM reservation
            WHERE customer_phone = #{phone}
              AND booking_code = #{bookingCode}
              AND reserved_at >= NOW() - INTERVAL 90 DAY
            ORDER BY reserved_at DESC
            """)
    List<Reservation> selectByPhoneAndCode(@Param("phone") String phone,
                                           @Param("bookingCode") String bookingCode);
}
