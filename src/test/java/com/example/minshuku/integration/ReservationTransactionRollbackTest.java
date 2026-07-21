package com.example.minshuku.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.ReservationGuest;
import com.example.minshuku.domain.Room;
import com.example.minshuku.mapper.ReservationGuestMapper;
import com.example.minshuku.mapper.ReservationMapper;
import com.example.minshuku.mapper.RoomMapper;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest
@LoggedTest
@DisplayName("予約トランザクションロールバック")
/**
 * 複数テーブルの更新途中で例外が発生した場合に、予約業務の更新がすべて取り消されることを確認する。
 */
class ReservationTransactionRollbackTest extends LocalDbTestSupport {
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ReservationMapper reservationMapper;
    @SpyBean
    private ReservationGuestMapper reservationGuestMapper;
    @SpyBean
    private RoomMapper roomMapper;

    @BeforeEach
    void setUp() {
        resetTables();
        seedRooms();
    }

    @AfterEach
    void tearDown() {
        resetTables();
    }

    @DisplayName("test_01 create Rolls Back Reservation When Companion Insert Fails")
    @Test
    void createRollsBackReservationWhenCompanionInsertFails() {
        Reservation reservation = baseReservation(
                bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2);
        doThrow(new IllegalStateException("同行者保存失敗"))
                .when(reservationGuestMapper)
                .insert(any(ReservationGuest.class));

        assertThatThrownBy(() -> reservationService.create(
                reservation,
                false,
                List.of("佐藤花子"),
                List.of("サトウハナコ"),
                List.of("女性"),
                List.of(28),
                List.of("080-0000-0000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("同行者保存失敗");

        assertThat(reservationService.countBooked()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_guests", Integer.class)).isZero();
        Room room = roomMapper.findById(bookableRoomId);
        assertThat(room.getOccupancyStatus()).isEqualTo("vacant");
        assertThat(room.getCleaningStatus()).isEqualTo("cleaned");
    }

    @DisplayName("test_02 sync Due Checkouts Rolls Back Reservation When Room Update Fails")
    @Test
    void syncDueCheckoutsRollsBackReservationWhenRoomUpdateFails() {
        LocalDate today = reservationService.currentDate();
        int reservationId = insertReservation(
                spareRoomId,
                "R000001",
                today.minusDays(2),
                today.minusDays(1),
                "山田太郎",
                "ヤマダタロウ",
                "男性",
                30,
                "090-0000-0000",
                "guest@example.com",
                1,
                "公式",
                "paid",
                "booked",
                BigDecimal.valueOf(12000),
                "ロールバック確認");
        roomMapper.updateStatuses(spareRoomId, "reserved", "cleaned");
        doThrow(new IllegalStateException("客室更新失敗"))
                .when(roomMapper)
                .updateStatuses(spareRoomId, "vacant", "needs_cleaning");

        assertThatThrownBy(() -> reservationService.syncDueCheckouts())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("客室更新失敗");

        Reservation reservation = reservationMapper.findById(reservationId);
        assertThat(reservation.getReservationStatus()).isEqualTo("booked");
        Room room = roomMapper.findById(spareRoomId);
        assertThat(room.getOccupancyStatus()).isEqualTo("reserved");
        assertThat(room.getCleaningStatus()).isEqualTo("cleaned");
    }

    private Reservation baseReservation(Integer roomId, LocalDate checkInDate, LocalDate checkOutDate, int guestCount) {
        Reservation reservation = TestSetData.reservation("standard").toDomain();
        reservation.setRoomId(roomId);
        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkOutDate);
        reservation.setGuestCount(guestCount);
        return reservation;
    }
}
