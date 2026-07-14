package com.pobar.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 時段可訂性。available 依查詢當下的 partySize + seatType 計算，
 * 不再回傳桌數 / 已訂數等內部容量細節（公開端點不需要）。
 */
@Data
@AllArgsConstructor
public class TimeSlotResponse {
    private String time;       // "17:00"
    private boolean available;
}
