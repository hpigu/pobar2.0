package com.pobar.dto.reservation;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequest {

    @NotBlank
    @Size(max = 50)
    private String guestName;

    @NotBlank
    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式不正確")
    private String guestPhone;

    @Min(1)
    @Max(50)
    private Integer partySize;

    @NotNull
    @Future(message = "預約時間必須為未來時間")
    private LocalDateTime reservedAt;

    @Size(max = 200)
    private String note;
}
