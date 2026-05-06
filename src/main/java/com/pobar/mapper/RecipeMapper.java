package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {

    @Select("SELECT * FROM recipe WHERE product_id = #{productId}")
    Recipe selectByProductId(Integer productId);
}
