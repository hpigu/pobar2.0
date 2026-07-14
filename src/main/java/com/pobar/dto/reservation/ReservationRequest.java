package com.pobar.dto.reservation;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequest {

    @NotBlank
    @Size(max = 50)
    private String customerName;

    @NotBlank
    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式不正確")
    private String customerPhone;

    @Min(1)
    @Max(50)
    private Integer partySize;

    /** 座位區：REGULAR（一般桌）或 BAR_COUNTER（吧台），null 視為 REGULAR */
    @Pattern(regexp = "^(REGULAR|BAR_COUNTER)$", message = "座位類型不正確")
    private String seatType;

    @NotNull
    @Future(message = "預約時間必須為未來時間")
    private LocalDateTime reservedAt;

    @Size(max = 200)
    private String notes;
}
