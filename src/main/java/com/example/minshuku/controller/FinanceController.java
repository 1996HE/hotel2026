package com.example.minshuku.controller;

import com.example.minshuku.domain.ReservationFinance;
import com.example.minshuku.service.FinanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 予約ごとの入金・返金と期間集計を提供する JSON API。 */
@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping
    public FinanceService.BusinessSummary summary(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return service.summary(startDate, endDate);
    }

    @GetMapping("/{reservationId}")
    public ReservationFinance find(@PathVariable Integer reservationId) {
        return service.find(reservationId);
    }

    @PostMapping("/{reservationId}/payment")
    public ReservationFinance payment(@PathVariable Integer reservationId, @RequestBody PaymentRequest request) {
        return service.recordPayment(reservationId, request.amount(), request.method(), request.occurredAt());
    }

    @PostMapping("/{reservationId}/refund")
    public ReservationFinance refund(@PathVariable Integer reservationId, @RequestBody RefundRequest request) {
        return service.recordRefund(reservationId, request.amount(), request.occurredAt());
    }

    public record PaymentRequest(BigDecimal amount, String method, OffsetDateTime occurredAt) {
    }
    public record RefundRequest(BigDecimal amount, OffsetDateTime occurredAt) {
    }
}
