package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.reservation.ReservationRequest;
import com.pobar.dto.reservation.ReservationResponse;
import com.pobar.dto.reservation.TimeSlotResponse;
import com.pobar.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // 顧客查詢可用時段（public）
    @GetMapping("/slots")
    public Result<List<TimeSlotResponse>> getSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.getSlots(date));
    }

    // 顧客自行預約（public）
    @PostMapping
    public Result<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return Result.ok(reservationService.create(request));
    }

    // 員工查詢指定日期的訂位清單
    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<List<ReservationResponse>> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.listByDate(date));
    }

    // 員工更新訂位狀態（CONFIRMED / SEATED / CANCELLED）
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<ReservationResponse> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Integer operatorId = (Integer) auth.getPrincipal();
        return Result.ok(reservationService.updateStatus(id, body.get("status"), operatorId));
    }
}
