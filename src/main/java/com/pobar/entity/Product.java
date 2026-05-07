package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer categoryId;
    private String nameZh;
    private String nameEn;
    private BigDecimal price;
    private String type;
    private String imageUrl;
    private Integer isActive;
    private Integer isAvailable;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
