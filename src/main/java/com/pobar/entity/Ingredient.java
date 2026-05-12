package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ingredient")
public class Ingredient {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String unit;
    private String category;         // BASE_SPIRIT / LIQUEUR / WINE / BEER / SYRUP / JUICE / FRESH / GARNISH / OTHER
    private Boolean isAvailable;     // 缺貨開關，連動下架相關酒品

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
