package com.example.minshuku.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.minshuku.service.PublicBookingService;
import com.example.minshuku.service.RoomCalendarService;
import com.example.minshuku.service.PublicBookingService.BookingLookupCancelRequest;
import com.example.minshuku.service.PublicBookingService.BookingLookupRequest;
import com.example.minshuku.service.PublicBookingService.BookingLookupView;
import com.example.minshuku.service.PublicBookingService.CreateRequest;
import com.example.minshuku.service.PublicBookingService.GuestInput;
import com.example.minshuku.service.PublicBookingService.RoomSelection;
import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupFailedException;
import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupRateLimitedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PublicBookingServiceLocalDbTest extends LocalDbTestSupport {
    @Autowired
    private PublicBookingService bookingService;
    @Autowired
    private RoomCalendarService calendarService;

    private int room101;
    private int room102;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() {
        resetTables();
        room101 = insertRoom("101", "山景双床房", "yoshitsu", 2, new BigDecimal("10000"), false,
                "vacant", "cleaned", true, null);
        room102 = insertRoom("102", "雪景和室", "washitsu", 3, new BigDecimal("15000"), false,
                "vacant", "cleaned", true, null);
        checkIn = LocalDate.now().plusDays(10);
        checkOut = checkIn.plusDays(2);
    }

    @Test
    void createHoldsMultipleRoomsAndLocksPrice() {
        PublicBookingService.CreateResult result = bookingService.create(request());

        assertTrue(result.requestNo().startsWith("BR"));
        assertEquals(new BigDecimal("70000.00"), result.totalAmount());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE booking_request_id IS NOT NULL AND reservation_status='pending'",
                Integer.class));
        assertEquals(3, jdbcTemplate.queryForObject("SELECT SUM(guest_count) FROM reservations", Integer.class));
        assertEquals(1, bookingService.pendingRequests().size());
        assertThrows(IllegalArgumentException.class, () -> bookingService.create(request()));
    }

    @Test
    void confirmChangesWholeRequestAndCreatesFinanceRows() {
        bookingService.create(request());
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM public_booking_requests", Integer.class);

        bookingService.confirm(id);

        assertEquals("confirmed", jdbcTemplate.queryForObject(
                "SELECT status FROM public_booking_requests WHERE id=?", String.class, id));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE booking_request_id=? AND reservation_status='booked'",
                Integer.class, id));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_finances", Integer.class));
    }

    @Test
    void adminCancellationCancelsConfirmedMultiRoomRequestAtomically() {
        bookingService.create(request());
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM public_booking_requests", Integer.class);
        bookingService.confirm(id);

        calendarService.cancelBookingRequest(id, "管理者による日程調整", "admin");

        assertEquals("cancelled", jdbcTemplate.queryForObject(
                "SELECT status FROM public_booking_requests WHERE id=?", String.class, id));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE booking_request_id=? AND reservation_status='cancelled'",
                Integer.class, id));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE booking_request_id=? AND cancellation_reason=?",
                Integer.class, id, "管理者による日程調整"));
    }

    @Test
    void rejectionRequiresReasonAndReleasesEveryRoom() {
        bookingService.create(request());
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM public_booking_requests", Integer.class);

        assertThrows(IllegalArgumentException.class, () -> bookingService.reject(id, " "));
        bookingService.reject(id, "満室調整のため");

        assertEquals("rejected", jdbcTemplate.queryForObject(
                "SELECT status FROM public_booking_requests WHERE id=?", String.class, id));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE booking_request_id=? AND reservation_status='cancelled'",
                Integer.class, id));
        assertEquals(2, bookingService.availableRooms(checkIn, checkOut).size());
    }

    @Test
    void secureLinkCancelsConfirmedRequestBeforeCutoff() {
        PublicBookingService.CreateResult created = bookingService.create(request());
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM public_booking_requests", Integer.class);
        bookingService.confirm(id);
        String token = created.cancellationUrl().substring(created.cancellationUrl().indexOf("token=") + 6);

        assertTrue(bookingService.cancellation(token).cancellable());
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancel(token, "", true));
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancel(token, "行程変更", false));
        bookingService.cancel(token, "行程変更", true);

        assertEquals("cancelled", jdbcTemplate.queryForObject(
                "SELECT status FROM public_booking_requests WHERE id=?", String.class, id));
        assertEquals("行程変更", jdbcTemplate.queryForObject(
                "SELECT cancellation_reason FROM public_booking_requests WHERE id=?", String.class, id));
        assertEquals(2, bookingService.availableRooms(checkIn, checkOut).size());
    }

    @Test
    void lookupReturnsWholeRequestAndRoomTotalsForMatchingCredentials() {
        PublicBookingService.CreateResult created = bookingService.create(request());

        BookingLookupView view = bookingService.lookup(
                new BookingLookupRequest(" " + created.requestNo().toLowerCase() + " ", "GUEST@EXAMPLE.COM"),
                "198.51.100.10");

        assertEquals(created.requestNo(), view.requestNo());
        assertEquals("pending", view.status());
        assertEquals(checkIn, view.checkInDate());
        assertEquals(checkOut, view.checkOutDate());
        assertEquals(3, view.guestCount());
        assertEquals(new BigDecimal("70000.00"), view.totalAmount());
        assertEquals(2, view.rooms().size());
        assertEquals("101", view.rooms().get(0).roomNumber());
        assertEquals("山景双床房", view.rooms().get(0).roomName());
        assertEquals(2, view.rooms().get(0).guestCount());
        assertEquals(new BigDecimal("40000.00"), view.rooms().get(0).totalAmount());
        assertTrue(view.cancellable());
        assertEquals(72, view.cancellationCutoffHours());
    }

    @Test
    void lookupBlocksSourceAfterFiveNonRevealingCredentialFailures() {
        PublicBookingService.CreateResult created = bookingService.create(request());
        BookingLookupRequest wrong = new BookingLookupRequest(created.requestNo(), "wrong@example.com");
        String source = "198.51.100.11";

        for (int attempt = 0; attempt < 5; attempt += 1) {
            assertThrows(LookupFailedException.class, () -> bookingService.lookup(wrong, source));
        }
        assertThrows(LookupRateLimitedException.class, () -> bookingService.lookup(
                new BookingLookupRequest(created.requestNo(), "guest@example.com"), source));

        BookingLookupView allowedFromAnotherSource = bookingService.lookup(
                new BookingLookupRequest(created.requestNo(), "guest@example.com"), "198.51.100.12");
        assertEquals(created.requestNo(), allowedFromAnotherSource.requestNo());
    }

    @Test
    void successfulLookupClearsEarlierFailuresForSource() {
        PublicBookingService.CreateResult created = bookingService.create(request());
        String source = "198.51.100.13";
        BookingLookupRequest wrong = new BookingLookupRequest(created.requestNo(), "wrong@example.com");

        for (int attempt = 0; attempt < 4; attempt += 1) {
            assertThrows(LookupFailedException.class, () -> bookingService.lookup(wrong, source));
        }
        bookingService.lookup(new BookingLookupRequest(created.requestNo(), "guest@example.com"), source);
        for (int attempt = 0; attempt < 5; attempt += 1) {
            assertThrows(LookupFailedException.class, () -> bookingService.lookup(wrong, source));
        }
        assertThrows(LookupRateLimitedException.class, () -> bookingService.lookup(wrong, source));
    }

    @Test
    void credentialsCancelWholeRequestAndReturnUpdatedView() {
        PublicBookingService.CreateResult created = bookingService.create(request());

        BookingLookupView cancelled = bookingService.cancelByLookup(
                new BookingLookupCancelRequest(
                        created.requestNo(), "guest@example.com", "旅行日程を変更したため", true),
                "198.51.100.14");

        assertEquals("cancelled", cancelled.status());
        assertEquals("旅行日程を変更したため", cancelled.cancellationReason());
        assertTrue(!cancelled.cancellable());
        assertEquals(2, cancelled.rooms().size());
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE reservation_status='cancelled'", Integer.class));
        assertEquals("旅行日程を変更したため", jdbcTemplate.queryForObject(
                "SELECT cancellation_reason FROM public_booking_requests", String.class));
    }

    @Test
    void credentialCancellationValidationNeverChangesTheBooking() {
        PublicBookingService.CreateResult created = bookingService.create(request());

        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelByLookup(
                new BookingLookupCancelRequest(created.requestNo(), "guest@example.com", " ", true),
                "198.51.100.15"));
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelByLookup(
                new BookingLookupCancelRequest(created.requestNo(), "guest@example.com", "x".repeat(1001), true),
                "198.51.100.15"));
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelByLookup(
                new BookingLookupCancelRequest(created.requestNo(), "guest@example.com", "予定変更", false),
                "198.51.100.15"));

        assertEquals("pending",
                jdbcTemplate.queryForObject("SELECT status FROM public_booking_requests", String.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservations WHERE reservation_status='pending'", Integer.class));
    }

    @Test
    void rejectedAndCutoffRequestsRemainReadOnly() {
        PublicBookingService.CreateResult rejectedCreated = bookingService.create(request());
        Integer rejectedId = jdbcTemplate.queryForObject("SELECT id FROM public_booking_requests", Integer.class);
        bookingService.reject(rejectedId, "受入不可");

        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelByLookup(
                new BookingLookupCancelRequest(
                        rejectedCreated.requestNo(), "guest@example.com", "予定変更", true),
                "198.51.100.16"));
        assertEquals("rejected",
                jdbcTemplate.queryForObject("SELECT status FROM public_booking_requests", String.class));

        resetTables();
        setUp();
        PublicBookingService.CreateResult cutoffCreated = bookingService.create(request());
        LocalDate nearCheckIn = LocalDate.now().plusDays(1);
        jdbcTemplate.update(
                "UPDATE public_booking_requests SET check_in_date=?, check_out_date=?",
                nearCheckIn,
                nearCheckIn.plusDays(1));

        BookingLookupView cutoffView = bookingService.lookup(
                new BookingLookupRequest(cutoffCreated.requestNo(), "guest@example.com"),
                "198.51.100.17");
        assertTrue(!cutoffView.cancellable());
        assertThrows(IllegalArgumentException.class, () -> bookingService.cancelByLookup(
                new BookingLookupCancelRequest(
                        cutoffCreated.requestNo(), "guest@example.com", "予定変更", true),
                "198.51.100.17"));
        assertEquals("pending",
                jdbcTemplate.queryForObject("SELECT status FROM public_booking_requests", String.class));
    }

    @Test
    void expirySchedulerReleasesPendingRooms() {
        bookingService.create(request());
        jdbcTemplate
                .update("UPDATE public_booking_requests SET hold_expires_at=DATEADD('HOUR', -1, CURRENT_TIMESTAMP)");

        assertEquals(1, bookingService.expirePending());
        assertEquals("expired",
                jdbcTemplate.queryForObject("SELECT status FROM public_booking_requests", String.class));
        assertEquals(2, bookingService.availableRooms(checkIn, checkOut).size());
    }

    private CreateRequest request() {
        return new CreateRequest(checkIn, checkOut,
                List.of(
                        new RoomSelection(room101, 2, List.of(
                                new GuestInput("代表者", "adult", null),
                                new GuestInput("子ども", "child", 10))),
                        new RoomSelection(room102, 1, List.of(new GuestInput("同行者", "adult", 25)))),
                "代表者", "guest@example.com", "090-0000-0000", "日本", "窓側希望", "zh", true, "");
    }
}
