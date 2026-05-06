package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("drink_attribute_option")
public class DrinkAttributeOption {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer attributeTypeId;
    private String nameZh;
    private String nameEn;
    private Integer displayOrder;

    @TableLogic
    private Integer isActive;
}
