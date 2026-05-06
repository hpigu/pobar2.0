package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.RecipeIngredient;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecipeIngredientMapper extends BaseMapper<RecipeIngredient> {

    @Select("SELECT * FROM recipe_ingredient WHERE recipe_id = #{recipeId} ORDER BY display_order")
    List<RecipeIngredient> selectByRecipeId(Integer recipeId);

    @Delete("DELETE FROM recipe_ingredient WHERE recipe_id = #{recipeId}")
    int deleteByRecipeId(Integer recipeId);
}
