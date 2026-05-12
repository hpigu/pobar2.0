package com.pobar.dto.table;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BarTableVO {
    private Integer id;
    private String name;
    private String type;
    private Integer capacity;
    private BigDecimal posX;
    private BigDecimal posY;
    private Boolean isLocked;

    // session 資訊（JOIN 而來）
    private String status;          // OPEN | CLOSED
    private Integer currentSessionId;
    private String sessionQrToken;
    private Integer partySize;
}
