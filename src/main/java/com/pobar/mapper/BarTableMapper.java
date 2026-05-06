package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.BarTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BarTableMapper extends BaseMapper<BarTable> {

    @Select("""
            SELECT bt.* FROM bar_table bt
            INNER JOIN table_session_table tst ON tst.table_id = bt.id
            WHERE tst.session_id = #{sessionId}
            """)
    List<BarTable> selectBySessionId(Integer sessionId);
}
