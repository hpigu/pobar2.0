package com.pobar.dto.reservation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationResponse {

    private Integer id;
    private String customerName;
    private String customerPhone;
    private Integer partySize;
    private LocalDateTime reservedAt;
    private String notes;
    private String status;
    /** 建立成功時回給顧客，後續搭配電話查詢 */
    private String bookingCode;
    private LocalDateTime createdAt;
}
