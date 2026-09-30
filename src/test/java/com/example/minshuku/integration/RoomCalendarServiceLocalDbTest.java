package com.example.minshuku.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.minshuku.service.InventoryConflictException;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.service.RoomCalendarService;
import com.example.minshuku.service.RoomCalendarService.BlockRelease;
import com.example.minshuku.service.RoomCalendarService.BlockUpdate;
import com.example.minshuku.service.RoomCalendarService.BlockWrite;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("房態カレンダーDB連携")
class RoomCalendarServiceLocalDbTest extends LocalDbTestSupport {
    @Autowired
    private RoomCalendarService calendarService;
    @Autowired
    private ReservationService reservationService;

    private LocalDate today;

    @BeforeEach
    void setUp() {
        resetTables();
        seedRooms();
        today = reservationService.currentDate();
    }

    @Test
    void calendarReturnsNaturalMonthStayAndEffectiveBlock() {
        YearMonth calendarMonth = YearMonth.from(today.plusMonths(1));
        LocalDate monthStart = calendarMonth.atDay(1);
        int reservationId = insertReservation(bookableRoomId, "R000001", monthStart.plusDays(1), monthStart.plusDays(3),
                "山田太郎", null, null, null, null, null, 1, "公式", "unpaid", "booked",
                new BigDecimal("12000"), null);
        calendarService.createBlock(new BlockWrite(spareRoomId, "maintenance", monthStart,
                monthStart.plusDays(1), "設備点検"), "admin");

        var view = calendarService.calendar(calendarMonth);

        assertThat(view.monthStart()).isEqualTo(calendarMonth.atDay(1));
        assertThat(view.nextMonthStart()).isEqualTo(calendarMonth.plusMonths(1).atDay(1));
        assertThat(view.stays()).extracting("id").contains(reservationId);
        assertThat(view.stays().stream().filter(stay -> stay.getId().equals(reservationId)).findFirst().orElseThrow()
                .getGroupSize()).isEqualTo(1);
        assertThat(view.blocks()).singleElement().satisfies(block -> {
            assertThat(block.getBlockType()).isEqualTo("maintenance");
            assertThat(block.getEndDateExclusive()).isEqualTo(monthStart.plusDays(1));
        });
    }

    @Test
    void middleReleaseSplitsBlockAndKeepsAuditTrail() {
        var created = calendarService.createBlock(new BlockWrite(bookableRoomId, "stop_sale", today.plusDays(1),
                today.plusDays(10), "販売調整"), "admin");

        var released = calendarService.releaseBlock(created.getId(), new BlockRelease(today.plusDays(4),
                today.plusDays(6), "一部再販", created.getVersion()), "admin");

        assertThat(released.fullyReleased()).isFalse();
        assertThat(released.remainingBlocks()).extracting("startDate", "endDateExclusive")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(today.plusDays(1), today.plusDays(4)),
                        org.assertj.core.groups.Tuple.tuple(today.plusDays(6), today.plusDays(10)));
        assertThat(calendarService.blockDetail(created.getId()).events()).extracting("eventType")
                .containsExactly("created", "partially_released");
    }

    @Test
    void blockConflictsWithReservationAndBlocksAvailability() {
        calendarService.createBlock(new BlockWrite(bookableRoomId, "stop_sale", today.plusDays(2),
                today.plusDays(5), "販売停止"), "admin");

        assertThat(reservationService.findAvailableRooms(today.plusDays(3), today.plusDays(4), 1))
                .extracting("id").doesNotContain(bookableRoomId);
        insertReservation(spareRoomId, "R000001", today.plusDays(2), today.plusDays(5), "佐藤花子", null,
                null, null, null, null, 1, "公式", "unpaid", "booked", new BigDecimal("12000"), null);
        assertThatThrownBy(() -> calendarService.createBlock(new BlockWrite(spareRoomId, "maintenance",
                today.plusDays(3), today.plusDays(4), "修理"), "admin"))
                .isInstanceOf(InventoryConflictException.class);
    }

    @Test
    void staleVersionAndPastWritesAreRejected() {
        var created = calendarService.createBlock(new BlockWrite(bookableRoomId, "maintenance", today.plusDays(1),
                today.plusDays(3), "点検"), "admin");
        calendarService.updateBlock(created.getId(), new BlockUpdate(bookableRoomId, "maintenance",
                today.plusDays(1), today.plusDays(4), "点検延長", created.getVersion()), "admin");

        assertThatThrownBy(() -> calendarService.updateBlock(created.getId(), new BlockUpdate(bookableRoomId,
                "maintenance", today.plusDays(1), today.plusDays(5), "古い更新", created.getVersion()), "admin"))
                .isInstanceOf(InventoryConflictException.class);
        assertThatThrownBy(() -> calendarService.createBlock(new BlockWrite(bookableRoomId, "stop_sale",
                today.minusDays(1), today.plusDays(1), "過去"), "admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
