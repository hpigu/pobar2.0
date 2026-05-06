package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("invoice")
public class Invoice {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer paymentId;
    private String invoiceNumber;
    private String carrierType;     // MOBILE_BARCODE, CITIZEN_CERT, PAPER
    private String carrierId;
    private String status;          // ISSUED, CANCELLED
    private LocalDateTime issuedAt;
}
