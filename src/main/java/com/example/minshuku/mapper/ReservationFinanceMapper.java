package com.example.minshuku.mapper;

import com.example.minshuku.domain.ReservationFinance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 入金、返金および月次集計を扱う MyBatis Mapper。 */
@Mapper
public interface ReservationFinanceMapper {
    ReservationFinance findByReservationId(@Param("reservationId") Integer reservationId);

    int insertEmpty(@Param("reservationId") Integer reservationId);

    int updatePayment(@Param("reservationId") Integer reservationId, @Param("amount") BigDecimal amount,
            @Param("method") String method, @Param("receivedAt") OffsetDateTime receivedAt);

    int updateRefund(@Param("reservationId") Integer reservationId, @Param("amount") BigDecimal amount,
            @Param("refundedAt") OffsetDateTime refundedAt);

    List<FinanceReportRow> findReportRows(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    record FinanceReportRow(Integer reservationId, String reservationNo, String guestName, String guestPhone,
            String roomNumber, LocalDate checkInDate, LocalDate checkOutDate, OffsetDateTime checkedInAt,
            OffsetDateTime checkedOutAt, Integer guestCount, BigDecimal totalAmount, BigDecimal receivedAmount,
            BigDecimal refundAmount, String paymentMethod, OffsetDateTime receivedAt, OffsetDateTime refundedAt,
            String reservationStatus, String paymentStatus) {
    }
}
