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
}
