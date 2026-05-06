package com.pobar.scheduler;

import com.pobar.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;

    // 每分鐘檢查一次，將超時 10 分鐘且未到場的訂位標記為 NO_SHOW
    @Scheduled(fixedRate = 60_000)
    public void checkNoShow() {
        reservationService.autoMarkNoShow();
    }
}
