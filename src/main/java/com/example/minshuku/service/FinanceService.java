package com.example.minshuku.service;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.ReservationFinance;
import com.example.minshuku.mapper.ReservationFinanceMapper;
import com.example.minshuku.mapper.ReservationFinanceMapper.FinanceReportRow;
import com.example.minshuku.mapper.ReservationMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 一予約一入金・一返金の制約と営業集計を扱う。 */
@Service
public class FinanceService {
    private static final Set<String> PAYMENT_METHODS = Set.of("cash", "card", "transfer", "platform", "unknown");
    private final ReservationFinanceMapper financeMapper;
    private final ReservationMapper reservationMapper;

    public FinanceService(ReservationFinanceMapper financeMapper, ReservationMapper reservationMapper) {
        this.financeMapper = financeMapper;
        this.reservationMapper = reservationMapper;
    }

    @Transactional(readOnly = true)
    public ReservationFinance find(Integer reservationId) {
        requireReservation(reservationId);
        ReservationFinance finance = financeMapper.findByReservationId(reservationId);
        return finance == null ? emptyFinance(reservationId) : finance;
    }

    @Transactional
    public ReservationFinance recordPayment(Integer reservationId, BigDecimal amount, String method,
            OffsetDateTime receivedAt) {
        Reservation reservation = requireReservation(reservationId);
        requireNonNegative(amount, "入金額");
        if (amount.signum() > 0 && !PAYMENT_METHODS.contains(method)) {
            throw new IllegalArgumentException("支払方法が正しくありません。");
        }
        ensureFinanceRow(reservationId);
        ReservationFinance current = financeMapper.findByReservationId(reservationId);
        if (current.getRefundAmount().compareTo(amount) > 0) {
            throw new IllegalArgumentException("入金額を返金額より少なくできません。");
        }
        financeMapper.updatePayment(reservationId, amount, amount.signum() == 0 ? null : method,
                amount.signum() == 0 ? null : (receivedAt == null ? OffsetDateTime.now() : receivedAt));
        synchronizePaymentStatus(reservation, amount, current.getRefundAmount());
        return financeMapper.findByReservationId(reservationId);
    }

    @Transactional
    public ReservationFinance recordRefund(Integer reservationId, BigDecimal amount, OffsetDateTime refundedAt) {
        Reservation reservation = requireReservation(reservationId);
        requireNonNegative(amount, "返金額");
        ensureFinanceRow(reservationId);
        ReservationFinance current = financeMapper.findByReservationId(reservationId);
        if (amount.compareTo(current.getReceivedAmount()) > 0) {
            throw new IllegalArgumentException("返金額は入金額を超えられません。");
        }
        financeMapper.updateRefund(reservationId, amount,
                amount.signum() == 0 ? null : (refundedAt == null ? OffsetDateTime.now() : refundedAt));
        synchronizePaymentStatus(reservation, current.getReceivedAmount(), amount);
        return financeMapper.findByReservationId(reservationId);
    }

    @Transactional(readOnly = true)
    public BusinessSummary summary(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        List<FinanceReportRow> rows = financeMapper.findReportRows(startDate, endDate);
        BigDecimal receivable = rows.stream().map(FinanceReportRow::totalAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal received = rows.stream().map(FinanceReportRow::receivedAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal refunded = rows.stream().map(FinanceReportRow::refundAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        return new BusinessSummary(receivable, received, refunded, received.subtract(refunded), rows);
    }

    private void synchronizePaymentStatus(Reservation reservation, BigDecimal received, BigDecimal refunded) {
        String status;
        if (refunded.signum() > 0)
            status = refunded.compareTo(received) == 0 ? "refunded" : "partially_refunded";
        else
            status = received.compareTo(reservation.getTotalAmount()) >= 0 ? "paid" : "unpaid";
        reservationMapper.updatePaymentStatus(reservation.getId(), status);
    }

    private void ensureFinanceRow(Integer reservationId) {
        if (financeMapper.findByReservationId(reservationId) == null)
            financeMapper.insertEmpty(reservationId);
    }

    private Reservation requireReservation(Integer id) {
        Reservation reservation = reservationMapper.findById(id);
        if (reservation == null)
            throw new IllegalArgumentException("予約が見つかりません。");
        return reservation;
    }

    private void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.signum() < 0)
            throw new IllegalArgumentException(label + "は0円以上にしてください。");
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("集計期間が正しくありません。");
        }
    }

    private ReservationFinance emptyFinance(Integer reservationId) {
        ReservationFinance finance = new ReservationFinance();
        finance.setReservationId(reservationId);
        finance.setReceivedAmount(BigDecimal.ZERO);
        finance.setRefundAmount(BigDecimal.ZERO);
        return finance;
    }

    public record BusinessSummary(BigDecimal receivable, BigDecimal received, BigDecimal refunded,
            BigDecimal netRevenue, List<FinanceReportRow> rows) {
    }
}
