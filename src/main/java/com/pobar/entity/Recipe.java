package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("recipe")
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer productId;
    private String preparationNotes;
}
