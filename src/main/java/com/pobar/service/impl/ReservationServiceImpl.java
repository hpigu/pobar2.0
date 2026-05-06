package com.pobar.service.impl;

import com.pobar.dto.reservation.ReservationRequest;
import com.pobar.dto.reservation.ReservationResponse;
import com.pobar.entity.Reservation;
import com.pobar.exception.BusinessException;
import com.pobar.logging.Audit;
import com.pobar.mapper.ReservationMapper;
import com.pobar.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    // 未到場自動取消：超過預約時間幾分鐘後標記 NO_SHOW
    private static final int NO_SHOW_GRACE_MINUTES = 10;

    private final ReservationMapper reservationMapper;

    @Override
    @Audit(action = "CREATE_RESERVATION", entityType = "Reservation")
    public ReservationResponse create(ReservationRequest request) {
        Reservation reservation = new Reservation();
        reservation.setGuestName(request.getGuestName());
        reservation.setGuestPhone(request.getGuestPhone());
        reservation.setPartySize(request.getPartySize());
        reservation.setReservedAt(request.getReservedAt());
        reservation.setNote(request.getNote());
        reservation.setStatus("PENDING");
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
        if (reservation == null) {
            throw new BusinessException(404, "找不到此訂位");
        }
        reservation.setStatus(status);
        reservation.setHandledBy(operatorId);
        reservationMapper.updateById(reservation);
        return toResponse(reservation);
    }

    @Override
    public void autoMarkNoShow() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(NO_SHOW_GRACE_MINUTES);
        int count = reservationMapper.markNoShow(cutoff);
        if (count > 0) {
            log.info("自動標記 {} 筆訂位為 NO_SHOW（寬限 {} 分鐘）", count, NO_SHOW_GRACE_MINUTES);
        }
    }

    private ReservationResponse toResponse(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.setId(r.getId());
        resp.setGuestName(r.getGuestName());
        resp.setGuestPhone(r.getGuestPhone());
        resp.setPartySize(r.getPartySize());
        resp.setReservedAt(r.getReservedAt());
        resp.setNote(r.getNote());
        resp.setStatus(r.getStatus());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
