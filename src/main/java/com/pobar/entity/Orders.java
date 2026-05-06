package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

// 避免與 SQL 保留字 ORDER 衝突，entity 命名 Orders，TableName 指向 orders
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer sessionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
