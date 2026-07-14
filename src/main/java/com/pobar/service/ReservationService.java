package com.pobar.service;

import com.pobar.dto.reservation.ReservationConfigResponse;
import com.pobar.dto.reservation.ReservationRequest;
import com.pobar.dto.reservation.ReservationResponse;
import com.pobar.dto.reservation.TimeSlotResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService {

    ReservationResponse create(ReservationRequest request);

    List<ReservationResponse> listByDate(LocalDate date);

    ReservationResponse updateStatus(Integer id, String status, Integer operatorId);

    void autoMarkNoShow();

    /** 依人數與座位區計算各時段可訂性（容量規則見 ReservationServiceImpl.canFit）。 */
    List<TimeSlotResponse> getSlots(LocalDate date, Integer partySize, String seatType);

    /** 訂位頁設定：座位區人數上限、可提前天數。 */
    ReservationConfigResponse getConfig();

    /** 顧客查詢：必須同時提供電話與訂位代碼（避免電話被列舉）。 */
    List<ReservationResponse> listByPhoneAndCode(String phone, String bookingCode);

    /** 顧客自助取消：以手機 + 訂位代碼驗證身分，僅 CONFIRMED 狀態可取消。 */
    ReservationResponse cancelByPhoneAndCode(String phone, String bookingCode);
}
