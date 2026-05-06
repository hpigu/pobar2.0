package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("bar_table")
public class BarTable {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String type;        // REGULAR, BAR_COUNTER
    private Integer capacity;
    private BigDecimal posX;
    private BigDecimal posY;
    private Integer isLocked;

    @TableLogic
    private Integer isActive;
}
