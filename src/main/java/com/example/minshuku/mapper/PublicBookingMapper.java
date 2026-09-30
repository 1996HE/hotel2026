package com.example.minshuku.mapper;

import com.example.minshuku.domain.PublicBookingRequest;
import com.example.minshuku.domain.Reservation;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 公開予約申請のヘッダーと、申請単位の予約明細を永続化する。 */
@Mapper
public interface PublicBookingMapper {
    long nextRequestSequence();

    int insert(PublicBookingRequest request);

    PublicBookingRequest findByIdForUpdate(@Param("id") Integer id);

    PublicBookingRequest findById(@Param("id") Integer id);

    PublicBookingRequest findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    PublicBookingRequest findByCredentials(
            @Param("requestNo") String requestNo,
            @Param("email") String email);

    PublicBookingRequest findByCredentialsForUpdate(
            @Param("requestNo") String requestNo,
            @Param("email") String email);

    List<PublicBookingRequest> findPending();

    List<PublicBookingRequest> findExpiredPendingForUpdate(@Param("now") OffsetDateTime now);

    List<Reservation> findReservations(@Param("bookingRequestId") Integer bookingRequestId);

    int updateStatus(
            @Param("id") Integer id,
            @Param("expectedStatus") String expectedStatus,
            @Param("status") String status,
            @Param("reason") String reason,
            @Param("now") OffsetDateTime now);

    int updateReservationsStatus(
            @Param("bookingRequestId") Integer bookingRequestId,
            @Param("expectedStatus") String expectedStatus,
            @Param("status") String status);

    int linkCustomer(@Param("bookingRequestId") Integer bookingRequestId, @Param("customerId") Integer customerId);

    int cancelReservations(
            @Param("bookingRequestId") Integer bookingRequestId,
            @Param("expectedStatus") String expectedStatus,
            @Param("reason") String reason,
            @Param("actor") String actor);
}
