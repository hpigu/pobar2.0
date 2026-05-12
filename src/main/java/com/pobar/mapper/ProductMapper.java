package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    // 當食材缺貨時，連動下架所有使用該食材的酒品
    @Update("""
            UPDATE product p
            INNER JOIN recipe r ON r.product_id = p.id
            INNER JOIN recipe_ingredient ri ON ri.recipe_id = r.id
            SET p.is_available = #{available}
            WHERE ri.ingredient_id = #{ingredientId}
            AND p.is_active = TRUE
            """)
    int updateAvailabilityByIngredient(@Param("ingredientId") Integer ingredientId,
                                       @Param("available") boolean available);

    // 查詢使用某食材的所有品項 ID（用於日誌記錄）
    List<Integer> selectProductIdsByIngredient(@Param("ingredientId") Integer ingredientId);
}
