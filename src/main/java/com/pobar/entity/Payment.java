package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer sessionId;
    private BigDecimal subtotal;
    private BigDecimal serviceChargeRate;
    private BigDecimal serviceCharge;
    private BigDecimal total;
    private String paymentMethod;   // CASH, CARD, OTHER
    private Integer splitCount;
    private BigDecimal amountPerPerson;
    private Integer processedBy;
    private LocalDateTime paidAt;
}
