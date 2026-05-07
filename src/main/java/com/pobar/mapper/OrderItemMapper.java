package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.dto.order.OrderItemDisplay;
import com.pobar.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    // 查詢某 session 的所有品項（JOIN product 取中文名稱）
    @Select("""
            SELECT oi.id, oi.order_id, oi.product_id, oi.quantity, oi.price,
                   oi.notes, oi.type, oi.status, oi.created_at, oi.updated_at,
                   p.name_zh AS productName,
                   o.session_id AS sessionId,
                   NULL AS tableNames,
                   NULL AS ingredientNames
            FROM order_item oi
            INNER JOIN orders o   ON o.id = oi.order_id
            INNER JOIN product p  ON p.id = oi.product_id
            WHERE o.session_id = #{sessionId}
            ORDER BY oi.created_at
            """)
    List<OrderItemDisplay> selectBySessionId(Integer sessionId);

    // 廚房/吧台取得待處理品項（JOIN 產品名稱、桌位名稱、酒譜食材）
    @Select("""
            SELECT oi.id, oi.order_id, oi.product_id, oi.quantity, oi.price,
                   oi.notes, oi.type, oi.status, oi.created_at, oi.updated_at,
                   p.name_zh                                              AS productName,
                   ts.id                                                  AS sessionId,
                   GROUP_CONCAT(DISTINCT bt.name ORDER BY bt.name SEPARATOR ', ') AS tableNames,
                   (SELECT GROUP_CONCAT(ing.name ORDER BY ri.display_order SEPARATOR ' · ')
                    FROM recipe r
                    JOIN recipe_ingredient ri ON ri.recipe_id = r.id
                    JOIN ingredient ing ON ing.id = ri.ingredient_id
                    WHERE r.product_id = oi.product_id)                   AS ingredientNames
            FROM order_item oi
            INNER JOIN orders o   ON o.id = oi.order_id
            INNER JOIN table_session ts ON ts.id = o.session_id
            INNER JOIN table_session_table tst ON tst.session_id = ts.id
            INNER JOIN bar_table bt ON bt.id = tst.table_id
            INNER JOIN product p  ON p.id = oi.product_id
            WHERE oi.type = #{type}
              AND oi.status IN ('PENDING','IN_PROGRESS')
              AND ts.status = 'OPEN'
            GROUP BY oi.id, p.name_zh, ts.id
            ORDER BY oi.created_at
            """)
    List<OrderItemDisplay> selectActiveByType(@Param("type") String type);

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
