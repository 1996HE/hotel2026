package com.example.minshuku.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 24時間を過ぎた未確認申請を定期的に取消し、客室保留を解放する。 */
@Component
public class BookingRequestScheduler {
    private final PublicBookingService bookingService;

    public BookingRequestScheduler(PublicBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(cron = "${app.public-booking.expiry-cron:20 * * * * *}", zone = "${app.time-zone:Asia/Tokyo}")
    public void expirePendingRequests() {
        bookingService.expirePending();
    }
}
