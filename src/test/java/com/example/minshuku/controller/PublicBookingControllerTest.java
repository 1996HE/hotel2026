package com.example.minshuku.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.minshuku.config.PublicBookingProperties;
import com.example.minshuku.config.SecurityConfig;
import com.example.minshuku.service.AdminUserService;
import com.example.minshuku.service.PublicBookingService;
import com.example.minshuku.service.PublicBookingService.BookingLookupCancelRequest;
import com.example.minshuku.service.PublicBookingService.BookingLookupRequest;
import com.example.minshuku.service.PublicBookingService.BookingLookupRoom;
import com.example.minshuku.service.PublicBookingService.BookingLookupView;
import com.example.minshuku.service.PublicBookingService.CreateResult;
import com.example.minshuku.service.PublicBookingService.PublicConfiguration;
import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupFailedException;
import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupRateLimitedException;
import com.example.minshuku.service.TurnstileVerifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicBookingController.class)
@Import({SecurityConfig.class, PublicBookingProperties.class})
class PublicBookingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicBookingService bookingService;

    @MockBean
    private TurnstileVerifier turnstileVerifier;

    @MockBean
    private AdminUserService adminUserService;

    @Test
    void publicConfigurationIsAvailableWithoutLogin() throws Exception {
        when(bookingService.configuration()).thenReturn(new PublicConfiguration(false, "", 24, 72));

        mockMvc.perform(get("/api/stay/config").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdHours").value(24))
                .andExpect(jsonPath("$.cancellationCutoffHours").value(72));
    }

    @Test
    void publicSubmissionDoesNotRequireCsrfToken() throws Exception {
        when(bookingService.create(any())).thenReturn(new CreateResult("BR00000001", "pending",
                OffsetDateTime.parse("2026-09-09T12:00:00+09:00"), new BigDecimal("10000"),
                "http://localhost/stay/cancel?token=test"));

        mockMvc.perform(post("/api/stay/bookings")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"checkInDate":"2026-10-01","checkOutDate":"2026-10-02","rooms":[],
                                 "leadName":"Guest","email":"g@example.com","phone":"090","country":"JP",
                                 "language":"en","consent":true,"turnstileToken":""}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestNo").value("BR00000001"));
    }

    @Test
    void lookupUsesPostBodyAndReturnsCompleteViewWithoutLoginOrCsrf() throws Exception {
        BookingLookupView view = new BookingLookupView(
                "BR00000001",
                "pending",
                LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-03"),
                2,
                new BigDecimal("40000.00"),
                OffsetDateTime.parse("2026-09-09T12:00:00+09:00"),
                null,
                null,
                true,
                72,
                List.of(new BookingLookupRoom("101", "Mountain Twin", 2, new BigDecimal("40000.00"))));
        when(bookingService.lookup(any(), eq("203.0.113.10"))).thenReturn(view);

        mockMvc.perform(post("/api/stay/bookings/lookup")
                .with(anonymous())
                .with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"requestNo":"BR00000001","email":"guest@example.com"}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.requestNo").value("BR00000001"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.guestCount").value(2))
                .andExpect(jsonPath("$.rooms[0].roomNumber").value("101"))
                .andExpect(jsonPath("$.rooms[0].totalAmount").value(40000.0));

        verify(bookingService).lookup(any(BookingLookupRequest.class), eq("203.0.113.10"));
    }

    @Test
    void credentialCancellationReturnsUpdatedViewWithoutLoginOrCsrf() throws Exception {
        BookingLookupView view = new BookingLookupView(
                "BR00000001",
                "cancelled",
                LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-03"),
                2,
                new BigDecimal("40000.00"),
                OffsetDateTime.parse("2026-09-09T12:00:00+09:00"),
                null,
                "Travel plans changed",
                false,
                72,
                List.of(new BookingLookupRoom("101", "Mountain Twin", 2, new BigDecimal("40000.00"))));
        when(bookingService.cancelByLookup(any(), eq("203.0.113.20"))).thenReturn(view);

        mockMvc.perform(post("/api/stay/bookings/lookup/cancel")
                .with(anonymous())
                .with(request -> {
                    request.setRemoteAddr("203.0.113.20");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"requestNo":"BR00000001","email":"guest@example.com",
                         "reason":"Travel plans changed","confirmed":true}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.status").value("cancelled"))
                .andExpect(jsonPath("$.cancellationReason").value("Travel plans changed"))
                .andExpect(jsonPath("$.cancellable").value(false));

        verify(bookingService).cancelByLookup(any(BookingLookupCancelRequest.class), eq("203.0.113.20"));
    }

    @Test
    void tokenCancellationRequiresReasonAndExplicitConfirmationBody() throws Exception {
        mockMvc.perform(post("/api/stay/bookings/cancel/test-token")
                .with(anonymous())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason":"Schedule changed","confirmed":true}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));

        verify(bookingService).cancel("test-token", "Schedule changed", true);
    }

    @Test
    void lookupCredentialFailureUsesNonRevealingCode() throws Exception {
        when(bookingService.lookup(any(), any())).thenThrow(new LookupFailedException());

        mockMvc.perform(post("/api/stay/bookings/lookup")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestNo":"BR00000001","email":"wrong@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("LOOKUP_FAILED"));
    }

    @Test
    void lookupBlockUsesRateLimitCode() throws Exception {
        when(bookingService.lookup(any(), any())).thenThrow(new LookupRateLimitedException());

        mockMvc.perform(post("/api/stay/bookings/lookup")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestNo":"BR00000001","email":"wrong@example.com"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("LOOKUP_RATE_LIMITED"));
    }

    @Test
    void adminRequestListRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/booking-requests/pending").with(anonymous()))
                .andExpect(status().isUnauthorized());
    }
}
