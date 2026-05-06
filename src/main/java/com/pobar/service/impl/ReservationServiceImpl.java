package com.pobar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pobar.dto.reservation.ReservationRequest;
import com.pobar.dto.reservation.ReservationResponse;
import com.pobar.dto.reservation.TimeSlotResponse;
import com.pobar.entity.BarTable;
import com.pobar.entity.Reservation;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.BarTableMapper;
import com.pobar.mapper.ReservationMapper;
import com.pobar.service.ReservationService;
import com.pobar.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final int NO_SHOW_GRACE_MINUTES = 10;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationMapper reservationMapper;
    private final BarTableMapper barTableMapper;
    private final SettingService settingService;

    @Override
    @Audit(action = "CREATE_RESERVATION", entityType = "Reservation")
    public ReservationResponse create(ReservationRequest request) {
        Reservation reservation = new Reservation();
        reservation.setCustomerName(request.getCustomerName());
        reservation.setCustomerPhone(request.getCustomerPhone());
        reservation.setPartySize(request.getPartySize());
        reservation.setReservedAt(request.getReservedAt());
        reservation.setNotes(request.getNotes());
        reservation.setSeatType("REGULAR");
        reservation.setDurationMinutes(120);
        reservation.setStatus("CONFIRMED");
        reservation.setCancelToken(UUID.randomUUID().toString());
        reservationMapper.insert(reservation);
        return toResponse(reservation);
    }

    @Override
    public List<ReservationResponse> listByDate(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        return reservationMapper.selectByDateRange(from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Audit(action = "UPDATE_RESERVATION_STATUS", entityType = "Reservation")
    public ReservationResponse updateStatus(Integer id, String status, Integer operatorId) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) throw new BusinessException(404, "找不到此訂位");
        reservation.setStatus(status);
        reservation.setHandledBy(operatorId);
        if ("CANCELLED".equals(status)) reservation.setCancelledAt(LocalDateTime.now());
        reservationMapper.updateById(reservation);
        return toResponse(reservation);
    }

    @Override
    public void autoMarkNoShow() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(NO_SHOW_GRACE_MINUTES);
        int count = reservationMapper.markNoShow(cutoff);
        if (count > 0) {
            log.info("自動取消 {} 筆逾時訂位（寬限 {} 分鐘）", count, NO_SHOW_GRACE_MINUTES);
        }
    }

    @Override
    public List<TimeSlotResponse> getSlots(LocalDate date) {
        LocalTime startTime = LocalTime.parse(settingService.get("food_service_start", "17:00"));
        LocalTime endTime   = LocalTime.parse(settingService.get("food_service_end",   "22:00"));
        int durationMin     = settingService.getInt("reservation_duration_minutes", 120);
        LocalTime lastSlot  = endTime.minusHours(1);

        long totalTables = barTableMapper.selectCount(
            new LambdaQueryWrapper<BarTable>().eq(BarTable::getIsActive, 1));

        // 查詢當天 +/- duration 範圍內的訂位（包含跨時段衝突）
        LocalDateTime from = date.atTime(startTime).minusMinutes(durationMin);
        LocalDateTime to   = date.atTime(lastSlot).plusMinutes(durationMin);
        List<Reservation> reservations = reservationMapper.selectActiveByDateRange(from, to);

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime slot = startTime;
        while (!slot.isAfter(lastSlot)) {
            final LocalDateTime slotDt = date.atTime(slot);
            long conflictCount = reservations.stream()
                .filter(r -> Math.abs(Duration.between(r.getReservedAt(), slotDt).toMinutes()) < durationMin)
                .count();
            slots.add(new TimeSlotResponse(slot.format(TIME_FMT), conflictCount < totalTables,
                    (int) conflictCount, (int) totalTables));
            slot = slot.plusMinutes(30);
        }
        return slots;
    }

    private ReservationResponse toResponse(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.setId(r.getId());
        resp.setCustomerName(r.getCustomerName());
        resp.setCustomerPhone(r.getCustomerPhone());
        resp.setPartySize(r.getPartySize());
        resp.setReservedAt(r.getReservedAt());
        resp.setNotes(r.getNotes());
        resp.setStatus(r.getStatus());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
