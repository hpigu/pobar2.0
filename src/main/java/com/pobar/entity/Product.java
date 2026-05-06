package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer categoryId;
    private String nameZh;
    private String nameEn;
    private String descriptionZh;
    private String descriptionEn;
    private BigDecimal price;
    private String type;
    private String imageUrl;
    private Integer isActive;
    private Integer isAvailable;
    private LocalTime availableStartTime;
    private LocalTime availableEndTime;
    private LocalDate availableFromDate;
    private LocalDate availableToDate;
    private Integer createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
