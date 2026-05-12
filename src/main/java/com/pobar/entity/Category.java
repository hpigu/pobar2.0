package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String nameZh;
    private String nameEn;
    private String type;
    private Integer displayOrder;

    @TableLogic
    private Boolean isActive;
}
