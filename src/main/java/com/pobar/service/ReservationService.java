package com.pobar.service;

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

    List<TimeSlotResponse> getSlots(LocalDate date);

    /** 顧客查詢：必須同時提供電話與訂位代碼（避免電話被列舉）。 */
    List<ReservationResponse> listByPhoneAndCode(String phone, String bookingCode);
}
