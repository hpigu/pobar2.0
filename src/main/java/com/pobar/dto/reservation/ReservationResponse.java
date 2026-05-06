package com.pobar.dto.reservation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationResponse {

    private Integer id;
    private String guestName;
    private String guestPhone;
    private Integer partySize;
    private LocalDateTime reservedAt;
    private String note;
    private String status;
    private LocalDateTime createdAt;
}
