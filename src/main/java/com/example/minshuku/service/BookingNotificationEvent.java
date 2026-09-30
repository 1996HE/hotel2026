package com.example.minshuku.service;

import com.example.minshuku.domain.PublicBookingRequest;

/** トランザクション確定後に送る予約申請通知。 */
public record BookingNotificationEvent(PublicBookingRequest request, String type, String cancellationUrl) {
}
