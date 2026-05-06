package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String guestName;
    private String guestPhone;
    private Integer partySize;
    private LocalDateTime reservedAt;    // 預約的時間（含日期）
    private String note;
    private String status;               // PENDING, CONFIRMED, SEATED, CANCELLED, NO_SHOW
    private Integer handledBy;           // 處理該訂位的員工 id

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
