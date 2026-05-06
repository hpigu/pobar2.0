package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("recipe_ingredient")
public class RecipeIngredient {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer recipeId;
    private Integer ingredientId;
    private BigDecimal quantity;
    private String unit;
    private Integer displayOrder;
}
