package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.reservation.ReservationRequest;
import com.pobar.dto.reservation.ReservationResponse;
import com.pobar.dto.reservation.TimeSlotResponse;
import com.pobar.security.AuthUser;
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

    // 顧客以手機 + 訂位代碼查自己的訂位（public，必須兩者皆提供）
    @GetMapping("/my")
    public Result<List<ReservationResponse>> listByPhoneAndCode(
            @RequestParam String phone,
            @RequestParam String code) {
        return Result.ok(reservationService.listByPhoneAndCode(phone, code));
    }

    // 顧客以手機 + 訂位代碼自助取消（public，用 POST body 避免記錄於 access log）
    @PostMapping("/cancel")
    public Result<ReservationResponse> cancel(@RequestBody Map<String, String> body) {
        return Result.ok(reservationService.cancelByPhoneAndCode(body.get("phone"), body.get("code")));
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
        Integer operatorId = ((AuthUser) auth.getPrincipal()).id();
        return Result.ok(reservationService.updateStatus(id, body.get("status"), operatorId));
    }
}
