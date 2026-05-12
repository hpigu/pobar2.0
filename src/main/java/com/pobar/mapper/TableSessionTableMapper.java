package com.pobar.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface TableSessionTableMapper {

    @Insert("INSERT INTO table_session_table (session_id, table_id) VALUES (#{sessionId}, #{tableId})")
    int insert(@Param("sessionId") Integer sessionId, @Param("tableId") Integer tableId);

    // 檢查某張桌子是否正在被使用（有 OPEN 的 session）
    @Select("""
            SELECT COUNT(1) FROM table_session_table tst
            INNER JOIN table_session ts ON ts.id = tst.session_id
            WHERE tst.table_id = #{tableId} AND ts.status = 'OPEN'
            """)
    int countActiveByTableId(Integer tableId);
}
