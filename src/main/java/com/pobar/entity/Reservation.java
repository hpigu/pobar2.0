package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String customerName;
    private String customerPhone;
    private String seatType;            // REGULAR, BAR_COUNTER（預設 REGULAR）
    private Integer partySize;
    private LocalDateTime reservedAt;
    private Integer durationMinutes;    // 預設 120
    private String status;              // CONFIRMED, SEATED, CANCELLED, NO_SHOW, COMPLETED
    private String cancelToken;         // UUID，顧客線上取消用
    private String bookingCode;         // 8 位英數，顧客查詢用（搭配電話）
    private Integer assignedTableId;
    private String notes;
    @TableField(exist = false)
    private Integer handledBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
}
