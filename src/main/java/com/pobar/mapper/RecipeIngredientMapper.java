package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.RecipeIngredient;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecipeIngredientMapper extends BaseMapper<RecipeIngredient> {

    @Select("SELECT * FROM recipe_ingredient WHERE recipe_id = #{recipeId} ORDER BY display_order")
    List<RecipeIngredient> selectByRecipeId(Integer recipeId);

    @Delete("DELETE FROM recipe_ingredient WHERE recipe_id = #{recipeId}")
    int deleteByRecipeId(Integer recipeId);

    @Select("""
            SELECT ri.id,
                   ri.recipe_id     AS recipeId,
                   ri.ingredient_id AS ingredientId,
                   i.name           AS ingredientName,
                   ri.quantity,
                   ri.unit,
                   ri.display_order AS displayOrder
            FROM recipe_ingredient ri
            JOIN ingredient i ON i.id = ri.ingredient_id
            WHERE ri.recipe_id = #{recipeId}
            ORDER BY ri.display_order
            """)
    List<Map<String, Object>> selectDetailByRecipeId(Integer recipeId);
}
