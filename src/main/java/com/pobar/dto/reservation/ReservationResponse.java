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
    private LocalDateTime createdAt;
}
