package com.pobar.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 訂位頁設定：前端據此決定座位區選項與人數上限。
 * <ul>
 *   <li>regularMaxPartySize = 可訂一般桌中最大單桌容量（不併桌，超過即不可訂，0 表示無一般桌）</li>
 *   <li>barCounterMaxPartySize = min(吧台單組人數上限設定, 吧台總座位數)，0 表示吧台不開放訂位</li>
 *   <li>maxAdvanceDays = 最多可提前幾天訂位</li>
 * </ul>
 */
@Data
@AllArgsConstructor
public class ReservationConfigResponse {
    private int regularMaxPartySize;
    private int barCounterMaxPartySize;
    private int maxAdvanceDays;
}
