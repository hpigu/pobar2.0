package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("table_session")
public class TableSession {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String qrToken;
    private String status;      // OPEN, CLOSED
    private Integer partySize;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Integer openedById;
}
