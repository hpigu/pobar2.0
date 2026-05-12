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
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.pobar.util.XssUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final int NO_SHOW_GRACE_MINUTES = 10;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    /** 訂位代碼字元集（去除易混淆的 0/O/1/I/L） */
    private static final char[] BOOKING_CODE_ALPHABET =
            "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int BOOKING_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservationMapper reservationMapper;
    private final BarTableMapper barTableMapper;
    private final SettingService settingService;

    @Override
    public ReservationResponse create(ReservationRequest request) {
        Reservation reservation = new Reservation();
        // XSS sanitize 所有字串欄位
        reservation.setCustomerName(XssUtil.sanitize(request.getCustomerName()));
        reservation.setCustomerPhone(XssUtil.sanitize(request.getCustomerPhone()));
        reservation.setPartySize(request.getPartySize());
        reservation.setReservedAt(request.getReservedAt());
        reservation.setNotes(XssUtil.sanitize(request.getNotes()));
        reservation.setSeatType("REGULAR");
        reservation.setDurationMinutes(120);
        reservation.setStatus("CONFIRMED");
        reservation.setCancelToken(UUID.randomUUID().toString());
        reservation.setBookingCode(generateBookingCode());
        reservationMapper.insert(reservation);
        return toResponse(reservation);
    }

    /** 產生 8 位易讀代碼，避免 0/O/1/I/L 混淆 */
    private String generateBookingCode() {
        StringBuilder sb = new StringBuilder(BOOKING_CODE_LENGTH);
        for (int i = 0; i < BOOKING_CODE_LENGTH; i++) {
            sb.append(BOOKING_CODE_ALPHABET[RANDOM.nextInt(BOOKING_CODE_ALPHABET.length)]);
        }
        return sb.toString();
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
    @Audit(action = "UPDATE_RESERVATION_STATUS", entityType = "Reservation",
            entityIdExpr = "#id",
            detailExpr = "'status=' + #status + ', operatorId=' + #operatorId")
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
            new LambdaQueryWrapper<BarTable>().eq(BarTable::getIsActive, true));

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

    @Override
    public List<ReservationResponse> listByPhoneAndCode(String phone, String bookingCode) {
        if (phone == null || phone.isBlank() || bookingCode == null || bookingCode.isBlank()) {
            throw new BusinessException(400, "請提供電話與訂位代碼");
        }
        return reservationMapper.selectByPhoneAndCode(phone.trim(), bookingCode.trim().toUpperCase()).stream()
                .map(this::toResponse)
                .toList();
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
        resp.setBookingCode(r.getBookingCode());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
