package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.dto.table.BarTableVO;
import com.pobar.entity.BarTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BarTableMapper extends BaseMapper<BarTable> {

    @Select("""
            SELECT bt.id, bt.name, bt.type, bt.capacity, bt.pos_x, bt.pos_y, bt.is_locked,
                   CASE WHEN ts.status = 'OPEN' THEN 'OPEN' ELSE 'CLOSED' END AS status,
                   ts.id   AS current_session_id,
                   ts.qr_token AS session_qr_token,
                   ts.party_size
            FROM bar_table bt
            LEFT JOIN table_session_table tst ON tst.table_id = bt.id
            LEFT JOIN table_session ts ON ts.id = tst.session_id AND ts.status = 'OPEN'
            WHERE bt.is_active = 1
            ORDER BY bt.id
            """)
    List<BarTableVO> listWithStatus();

    @Select("""
            SELECT bt.* FROM bar_table bt
            INNER JOIN table_session_table tst ON tst.table_id = bt.id
            WHERE tst.session_id = #{sessionId}
            """)
    List<BarTable> selectBySessionId(Integer sessionId);

    // 可訂位桌位（active 且未鎖定），加 FOR UPDATE 序列化並發訂位的「檢查 + 寫入」，
    // 防止兩筆同時通過容量檢查造成超訂。必須在交易內呼叫。
    @Select("""
            SELECT * FROM bar_table
            WHERE is_active = 1 AND is_locked = 0
            ORDER BY id
            FOR UPDATE
            """)
    List<BarTable> selectReservableForUpdate();
}
