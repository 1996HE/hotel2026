package com.example.minshuku.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 一予約につき一回の入金と一回の返金を保持する会計明細。
 */
public class ReservationFinance {
    private Integer id;
    private Integer reservationId;
    private BigDecimal receivedAmount;
    private String paymentMethod;
    private OffsetDateTime receivedAt;
    private BigDecimal refundAmount;
    private OffsetDateTime refundedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public OffsetDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(OffsetDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** 実収入は入金額から返金額を差し引いた値。 */
    public BigDecimal getNetAmount() {
        BigDecimal received = receivedAmount == null ? BigDecimal.ZERO : receivedAmount;
        BigDecimal refunded = refundAmount == null ? BigDecimal.ZERO : refundAmount;
        return received.subtract(refunded);
    }
}
