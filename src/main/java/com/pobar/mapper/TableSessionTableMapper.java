package com.pobar.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TableSessionTableMapper {

    @Insert("INSERT INTO table_session_table (session_id, table_id) VALUES (#{sessionId}, #{tableId})")
    int insert(@Param("sessionId") Integer sessionId, @Param("tableId") Integer tableId);

    @Delete("DELETE FROM table_session_table WHERE session_id = #{sessionId}")
    int deleteBySessionId(Integer sessionId);

    @Select("SELECT table_id FROM table_session_table WHERE session_id = #{sessionId}")
    List<Integer> selectTableIdsBySessionId(Integer sessionId);

    // 檢查某張桌子是否正在被使用（有 OPEN 的 session）
    @Select("""
            SELECT COUNT(1) FROM table_session_table tst
            INNER JOIN table_session ts ON ts.id = tst.session_id
            WHERE tst.table_id = #{tableId} AND ts.status = 'OPEN'
            """)
    int countActiveByTableId(Integer tableId);
}
