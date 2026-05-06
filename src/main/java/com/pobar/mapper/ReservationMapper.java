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

    // 自動取消：找出超過指定時間仍 PENDING/CONFIRMED 的訂位
    @Update("""
            UPDATE reservation
            SET status = 'NO_SHOW', updated_at = NOW()
            WHERE status IN ('PENDING','CONFIRMED')
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
}
