package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("drink_attribute_type")
public class DrinkAttributeType {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String nameZh;
    private String nameEn;
    private Integer displayOrder;

    @TableLogic
    private Integer isActive;
}
