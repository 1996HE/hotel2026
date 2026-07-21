package com.example.minshuku.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 期限を迎えた予約のチェックアウト状態を定期的に同期する。 */
@Component
public class CheckoutScheduler {
    private final ReservationService reservationService;

    public CheckoutScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "${app.checkout-sync-cron:0 0 * * * *}", zone = "${app.time-zone:Asia/Tokyo}")
    public void syncDueCheckouts() {
        reservationService.syncDueCheckouts();
    }
}
