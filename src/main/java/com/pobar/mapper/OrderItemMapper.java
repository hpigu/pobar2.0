package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    // 查詢某 session 的所有品項（JOIN orders 取 session_id）
    @Select("""
            SELECT oi.* FROM order_item oi
            INNER JOIN orders o ON o.id = oi.order_id
            WHERE o.session_id = #{sessionId}
            ORDER BY oi.created_at
            """)
    List<OrderItem> selectBySessionId(Integer sessionId);

    // 廚房/吧台取得待處理品項（依類型篩選，排除已取消）
    @Select("""
            SELECT oi.* FROM order_item oi
            INNER JOIN orders o ON o.id = oi.order_id
            INNER JOIN table_session ts ON ts.id = o.session_id
            WHERE oi.type = #{type}
              AND oi.status != 'CANCELLED'
              AND ts.status = 'OPEN'
            ORDER BY oi.created_at
            """)
    List<OrderItem> selectActiveByType(@Param("type") String type);

    // 銷售排行（指定時間區間，依銷售量排序）
    @Select("""
            SELECT
              oi.product_id                               AS productId,
              p.name_zh                                   AS productName,
              c.name_zh                                   AS categoryName,
              SUM(oi.quantity)                            AS totalQuantity,
              SUM(oi.price * oi.quantity)                 AS totalRevenue
            FROM order_item oi
            INNER JOIN orders o ON o.id = oi.order_id
            INNER JOIN product p ON p.id = oi.product_id
            INNER JOIN category c ON c.id = p.category_id
            WHERE oi.status != 'CANCELLED'
              AND o.created_at BETWEEN #{from} AND #{to}
            GROUP BY oi.product_id, p.name_zh, c.name_zh
            ORDER BY totalQuantity DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectSalesRanking(@Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to,
                                                  @Param("limit") int limit);
}
