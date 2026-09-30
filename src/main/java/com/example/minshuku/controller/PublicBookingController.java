package com.example.minshuku.controller;

import com.example.minshuku.service.InventoryConflictException;
import com.example.minshuku.service.PublicBookingService;
import com.example.minshuku.service.PublicBookingService.AdminBooking;
import com.example.minshuku.service.PublicBookingService.BookingLookupCancelRequest;
import com.example.minshuku.service.PublicBookingService.BookingLookupRequest;
import com.example.minshuku.service.PublicBookingService.BookingLookupView;
import com.example.minshuku.service.PublicBookingService.CancellationView;
import com.example.minshuku.service.PublicBookingService.CreateRequest;
import com.example.minshuku.service.PublicBookingService.CreateResult;
import com.example.minshuku.service.PublicBookingService.PublicConfiguration;
import com.example.minshuku.service.PublicBookingService.Quote;
import com.example.minshuku.service.PublicBookingService.QuoteRequest;
import com.example.minshuku.service.PublicBookingService.RoomOption;
import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupFailedException;
import com.example.minshuku.service.PublicBookingLookupRateLimiter.LookupRateLimitedException;
import com.example.minshuku.service.TurnstileVerifier;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 客向け予約 API と、認証済み管理者の申請確認 API。 */
@RestController
@RequestMapping("/api")
public class PublicBookingController {
    private final PublicBookingService bookingService;
    private final TurnstileVerifier turnstileVerifier;

    public PublicBookingController(PublicBookingService bookingService, TurnstileVerifier turnstileVerifier) {
        this.bookingService = bookingService;
        this.turnstileVerifier = turnstileVerifier;
    }

    @GetMapping("/stay/config")
    public PublicConfiguration configuration() {
        return bookingService.configuration();
    }

    @GetMapping("/stay/rooms")
    public List<RoomOption> rooms(@RequestParam LocalDate checkInDate, @RequestParam LocalDate checkOutDate) {
        return bookingService.availableRooms(checkInDate, checkOutDate);
    }

    @PostMapping("/stay/quote")
    public Quote quote(@RequestBody QuoteRequest request) {
        return bookingService.quote(request);
    }

    @PostMapping("/stay/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResult create(@RequestBody CreateRequest request, HttpServletRequest servletRequest) {
        turnstileVerifier.verify(request.turnstileToken(), servletRequest.getRemoteAddr());
        return bookingService.create(request);
    }

    @PostMapping("/stay/bookings/lookup")
    public BookingLookupView lookup(
            @RequestBody BookingLookupRequest request,
            HttpServletRequest servletRequest) {
        return bookingService.lookup(request, servletRequest.getRemoteAddr());
    }

    @PostMapping("/stay/bookings/lookup/cancel")
    public BookingLookupView cancelByLookup(
            @RequestBody BookingLookupCancelRequest request,
            HttpServletRequest servletRequest) {
        return bookingService.cancelByLookup(request, servletRequest.getRemoteAddr());
    }

    @GetMapping("/stay/bookings/cancel/{token}")
    public CancellationView cancellation(@PathVariable String token) {
        return bookingService.cancellation(token);
    }

    @PostMapping("/stay/bookings/cancel/{token}")
    public MessageResponse cancel(@PathVariable String token, @RequestBody CancellationRequest request) {
        bookingService.cancel(token, request.reason(), request.confirmed());
        return new MessageResponse("预约已取消，房间已经释放。");
    }

    @GetMapping("/booking-requests/pending")
    public List<AdminBooking> pending() {
        return bookingService.pendingRequests();
    }

    @PostMapping("/booking-requests/{id}/confirm")
    public MessageResponse confirm(@PathVariable Integer id) {
        bookingService.confirm(id);
        return new MessageResponse("预约申请已整体确认。");
    }

    @PostMapping("/booking-requests/{id}/reject")
    public MessageResponse reject(@PathVariable Integer id, @RequestBody RejectRequest request) {
        bookingService.reject(id, request.reason());
        return new MessageResponse("预约申请已整体拒绝，房间已经释放。");
    }

    @ExceptionHandler(LookupFailedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleLookupFailed() {
        return new ErrorResponse("LOOKUP_FAILED");
    }

    @ExceptionHandler(LookupRateLimitedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleLookupRateLimited() {
        return new ErrorResponse("LOOKUP_RATE_LIMITED");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(RuntimeException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler({InventoryConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException ex) {
        return new ErrorResponse(ex instanceof InventoryConflictException
                ? ex.getMessage()
                : "房间库存刚刚发生变化，请刷新后重试。");
    }

    public record RejectRequest(String reason) {
    }
    public record CancellationRequest(String reason, boolean confirmed) {
    }
    public record MessageResponse(String message) {
    }
    public record ErrorResponse(String error) {
    }
}
