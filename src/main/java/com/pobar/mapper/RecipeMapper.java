package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {

    @Select("SELECT * FROM recipe WHERE product_id = #{productId}")
    Recipe selectByProductId(Integer productId);

    @Select("""
            SELECT i.name
            FROM ingredient i
            JOIN recipe_ingredient ri ON ri.ingredient_id = i.id
            JOIN recipe r ON r.id = ri.recipe_id
            WHERE r.product_id = #{productId}
            ORDER BY ri.display_order
            """)
    List<String> selectIngredientNamesByProductId(Integer productId);
}
