package com.pobar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pobar.dto.reservation.ReservationConfigResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
    /** 訂位狀態白名單，避免任意字串寫入 */
    private static final Set<String> ALLOWED_STATUSES =
            Set.of("CONFIRMED", "SEATED", "CANCELLED", "NO_SHOW", "COMPLETED");

    private static final String SEAT_REGULAR = "REGULAR";
    private static final String SEAT_BAR = "BAR_COUNTER";
    /** 時段格線（分鐘），訂位時間必須對齊 */
    private static final int SLOT_GRID_MINUTES = 30;
    /**
     * 撈重疊訂位時往前回看的分鐘數：取 max(目前 duration 設定, 此值)，
     * 涵蓋歷史上以較長 duration 建立的舊訂位，避免漏算跨時段佔用。
     */
    private static final int MAX_DURATION_LOOKBACK_MINUTES = 360;

    private final ReservationMapper reservationMapper;
    private final BarTableMapper barTableMapper;
    private final SettingService settingService;

    /**
     * 建立訂位（防超訂核心）。
     * 整段包在交易內，先以 SELECT ... FOR UPDATE 鎖住可訂桌位，
     * 把並發訂位的「容量檢查 + 寫入」序列化，杜絕兩筆同時通過檢查造成超訂。
     */
    @Override
    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        String seatType = normalizeSeatType(request.getSeatType());
        int partySize = request.getPartySize();
        LocalDateTime reservedAt = request.getReservedAt();
        int durationMin = settingService.getInt("reservation_duration_minutes", 120);

        validateReservedAt(reservedAt);

        // 鎖住可訂桌位（active 且未鎖定），序列化並發的容量檢查
        List<BarTable> tables = barTableMapper.selectReservableForUpdate();
        validatePartySize(seatType, partySize, tables);

        // 撈出與 [reservedAt, reservedAt + duration) 重疊、仍佔容量的訂位
        List<Reservation> overlapping = findOverlapping(reservedAt, durationMin);
        if (!canFit(tables, overlapping, seatType, partySize)) {
            throw new BusinessException("該時段座位已滿，請選擇其他時段");
        }

        Reservation reservation = new Reservation();
        // XSS sanitize 所有字串欄位
        reservation.setCustomerName(XssUtil.sanitize(request.getCustomerName()));
        reservation.setCustomerPhone(XssUtil.sanitize(request.getCustomerPhone()));
        reservation.setPartySize(partySize);
        reservation.setReservedAt(reservedAt);
        reservation.setNotes(XssUtil.sanitize(request.getNotes()));
        reservation.setSeatType(seatType);
        reservation.setDurationMinutes(durationMin);
        reservation.setStatus("CONFIRMED");
        reservation.setBookingCode(generateBookingCode());
        reservationMapper.insert(reservation);
        return toResponse(reservation);
    }

    /** null 視為 REGULAR；其餘僅接受白名單值（@Pattern 已擋，此處為服務層防線）。 */
    private String normalizeSeatType(String seatType) {
        if (seatType == null || seatType.isBlank()) return SEAT_REGULAR;
        if (!SEAT_REGULAR.equals(seatType) && !SEAT_BAR.equals(seatType)) {
            throw new BusinessException("座位類型不正確");
        }
        return seatType;
    }

    /** 驗證訂位時間：30 分鐘格線、未來時間、提前天數上限、營業時段內。 */
    private void validateReservedAt(LocalDateTime reservedAt) {
        if (reservedAt.getMinute() % SLOT_GRID_MINUTES != 0
                || reservedAt.getSecond() != 0 || reservedAt.getNano() != 0) {
            throw new BusinessException("訂位時間須為 30 分鐘整的時段");
        }
        if (!reservedAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException("預約時間必須為未來時間");
        }
        int maxAdvanceDays = settingService.getInt("reservation_max_advance_days", 10);
        if (reservedAt.toLocalDate().isAfter(LocalDate.now().plusDays(maxAdvanceDays))) {
            throw new BusinessException("最多可提前 " + maxAdvanceDays + " 天訂位");
        }
        LocalTime start = LocalTime.parse(settingService.get("food_service_start", "17:00"));
        LocalTime lastSlot = LocalTime.parse(settingService.get("food_service_end", "22:00")).minusHours(1);
        LocalTime time = reservedAt.toLocalTime();
        if (time.isBefore(start) || time.isAfter(lastSlot)) {
            throw new BusinessException("訂位時間須介於 " + start.format(TIME_FMT)
                    + " 至 " + lastSlot.format(TIME_FMT) + " 之間");
        }
    }

    /** 依座位區驗證人數：吧台限 1~上限（預設 3）；一般桌不併桌，人數不得超過最大單桌容量。 */
    private void validatePartySize(String seatType, int partySize, List<BarTable> tables) {
        if (partySize < 1) throw new BusinessException("人數不正確");
        if (SEAT_BAR.equals(seatType)) {
            int barMax = barCounterMaxParty(tables);
            if (barMax <= 0) throw new BusinessException("吧台目前不開放訂位");
            if (partySize > barMax) {
                throw new BusinessException("吧台訂位最多 " + barMax + " 位，人數較多請改訂一般座位");
            }
        } else {
            int maxTableCapacity = tables.stream()
                    .filter(t -> !SEAT_BAR.equals(t.getType()))
                    .mapToInt(BarTable::getCapacity).max().orElse(0);
            if (maxTableCapacity <= 0) throw new BusinessException("目前無可訂位的座位");
            if (partySize > maxTableCapacity) {
                throw new BusinessException("線上訂位單組最多 " + maxTableCapacity + " 位，人數較多請來電洽詢");
            }
        }
    }

    /** 吧台單組人數上限 = min(設定值, 吧台總座位數)。 */
    private int barCounterMaxParty(List<BarTable> tables) {
        int barSeats = tables.stream()
                .filter(t -> SEAT_BAR.equals(t.getType()))
                .mapToInt(BarTable::getCapacity).sum();
        return Math.min(settingService.getInt("bar_counter_max_party", 3), barSeats);
    }

    /** 撈出與 [start, start + durationMin) 區間重疊、仍佔容量（CONFIRMED/SEATED）的訂位。 */
    private List<Reservation> findOverlapping(LocalDateTime start, int durationMin) {
        LocalDateTime end = start.plusMinutes(durationMin);
        LocalDateTime from = start.minusMinutes(Math.max(durationMin, MAX_DURATION_LOOKBACK_MINUTES));
        return reservationMapper.selectActiveByDateRange(from, end).stream()
                .filter(r -> overlaps(r, start, end))
                .toList();
    }

    /** 區間重疊判定 [start, end)，使用每筆訂位自己的 durationMinutes。 */
    private boolean overlaps(Reservation r, LocalDateTime start, LocalDateTime end) {
        int duration = r.getDurationMinutes() != null ? r.getDurationMinutes() : 120;
        LocalDateTime rStart = r.getReservedAt();
        LocalDateTime rEnd = rStart.plusMinutes(duration);
        return rStart.isBefore(end) && start.isBefore(rEnd);
    }

    /**
     * 容量檢查。
     * <ul>
     *   <li>吧台（BAR_COUNTER）：座位池——該時段吧台訂位人數加總 + 本組 ≤ 吧台總座位數。</li>
     *   <li>一般桌（REGULAR）：不併桌，每組需一張 capacity ≥ 人數的桌子。
     *       用 best-fit decreasing 檢查：組別由大到小，各拿「仍容納得下的最小桌」；
     *       在不併桌規則下此貪婪法必然找到可行解（若存在），不會誤判。</li>
     * </ul>
     */
    private boolean canFit(List<BarTable> tables, List<Reservation> overlapping,
                           String seatType, int partySize) {
        if (SEAT_BAR.equals(seatType)) {
            int barSeats = tables.stream()
                    .filter(t -> SEAT_BAR.equals(t.getType()))
                    .mapToInt(BarTable::getCapacity).sum();
            int reservedSeats = overlapping.stream()
                    .filter(r -> SEAT_BAR.equals(r.getSeatType()))
                    .mapToInt(Reservation::getPartySize).sum();
            return reservedSeats + partySize <= barSeats;
        }
        // 一般桌：桌容量由小到大、組別由大到小，各組取仍塞得下的最小桌
        List<Integer> capacities = new ArrayList<>(tables.stream()
                .filter(t -> !SEAT_BAR.equals(t.getType()))
                .map(BarTable::getCapacity)
                .sorted()
                .toList());
        List<Integer> parties = new ArrayList<>(overlapping.stream()
                .filter(r -> !SEAT_BAR.equals(r.getSeatType()))
                .map(Reservation::getPartySize)
                .toList());
        parties.add(partySize);
        parties.sort(Comparator.reverseOrder());
        for (int party : parties) {
            int chosen = -1;
            for (int i = 0; i < capacities.size(); i++) {
                if (capacities.get(i) >= party) { chosen = i; break; }
            }
            if (chosen < 0) return false;
            capacities.remove(chosen);
        }
        return true;
    }

    /** 可訂桌位（active 且未鎖定），無鎖版本供查詢用。 */
    private List<BarTable> reservableTables() {
        return barTableMapper.selectList(
                new LambdaQueryWrapper<BarTable>().eq(BarTable::getIsLocked, false));
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
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException("不合法的訂位狀態：" + status);
        }
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
    public List<TimeSlotResponse> getSlots(LocalDate date, Integer partySize, String seatType) {
        String seat = normalizeSeatType(seatType);
        int party = (partySize == null || partySize < 1) ? 1 : partySize;

        LocalTime startTime = LocalTime.parse(settingService.get("food_service_start", "17:00"));
        LocalTime endTime   = LocalTime.parse(settingService.get("food_service_end",   "22:00"));
        int durationMin     = settingService.getInt("reservation_duration_minutes", 120);
        LocalTime lastSlot  = endTime.minusHours(1);

        List<BarTable> tables = reservableTables();

        // 一次撈出可能與當天任一時段重疊的訂位，逐時段在記憶體過濾
        LocalDateTime from = date.atTime(startTime)
                .minusMinutes(Math.max(durationMin, MAX_DURATION_LOOKBACK_MINUTES));
        LocalDateTime to   = date.atTime(lastSlot).plusMinutes(durationMin);
        List<Reservation> reservations = reservationMapper.selectActiveByDateRange(from, to);

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime slot = startTime;
        while (!slot.isAfter(lastSlot)) {
            LocalDateTime slotStart = date.atTime(slot);
            LocalDateTime slotEnd = slotStart.plusMinutes(durationMin);
            List<Reservation> overlapping = reservations.stream()
                    .filter(r -> overlaps(r, slotStart, slotEnd))
                    .toList();
            slots.add(new TimeSlotResponse(slot.format(TIME_FMT),
                    canFit(tables, overlapping, seat, party)));
            slot = slot.plusMinutes(SLOT_GRID_MINUTES);
        }
        return slots;
    }

    @Override
    public ReservationConfigResponse getConfig() {
        List<BarTable> tables = reservableTables();
        int regularMax = tables.stream()
                .filter(t -> !SEAT_BAR.equals(t.getType()))
                .mapToInt(BarTable::getCapacity).max().orElse(0);
        int barMax = barCounterMaxParty(tables);
        int maxAdvanceDays = settingService.getInt("reservation_max_advance_days", 10);
        return new ReservationConfigResponse(regularMax, Math.max(barMax, 0), maxAdvanceDays);
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

    @Override
    @Audit(action = "CANCEL_RESERVATION", entityType = "Reservation", entityIdExpr = "#result?.id",
            allowAnonymous = true)
    public ReservationResponse cancelByPhoneAndCode(String phone, String bookingCode) {
        if (phone == null || phone.isBlank() || bookingCode == null || bookingCode.isBlank()) {
            throw new BusinessException(400, "請提供電話與訂位代碼");
        }
        List<Reservation> matches =
                reservationMapper.selectByPhoneAndCode(phone.trim(), bookingCode.trim().toUpperCase());
        if (matches.isEmpty()) {
            throw new BusinessException(404, "找不到符合的訂位");
        }
        Reservation reservation = matches.get(0);
        if (!"CONFIRMED".equals(reservation.getStatus())) {
            throw new BusinessException("此訂位無法取消");
        }
        reservation.setStatus("CANCELLED");
        reservation.setCancelledAt(LocalDateTime.now());
        reservationMapper.updateById(reservation);
        return toResponse(reservation);
    }

    private ReservationResponse toResponse(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.setId(r.getId());
        resp.setCustomerName(r.getCustomerName());
        resp.setCustomerPhone(r.getCustomerPhone());
        resp.setPartySize(r.getPartySize());
        resp.setSeatType(r.getSeatType());
        resp.setReservedAt(r.getReservedAt());
        resp.setNotes(r.getNotes());
        resp.setStatus(r.getStatus());
        resp.setBookingCode(r.getBookingCode());
        resp.setCreatedAt(r.getCreatedAt());
        return resp;
    }
}
