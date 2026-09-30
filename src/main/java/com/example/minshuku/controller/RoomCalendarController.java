package com.example.minshuku.controller;

import com.example.minshuku.domain.RoomInventoryBlock;
import com.example.minshuku.service.InventoryConflictException;
import com.example.minshuku.service.RoomCalendarService;
import com.example.minshuku.service.RoomCalendarService.BlockDetail;
import com.example.minshuku.service.RoomCalendarService.BlockRelease;
import com.example.minshuku.service.RoomCalendarService.BlockUpdate;
import com.example.minshuku.service.RoomCalendarService.BlockWrite;
import com.example.minshuku.service.RoomCalendarService.CalendarView;
import com.example.minshuku.service.RoomCalendarService.ReleaseResult;
import com.example.minshuku.service.RoomCalendarService.ReservationDetail;
import java.security.Principal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated JSON endpoints for the admin room-state calendar. */
@RestController
@RequestMapping("/api")
public class RoomCalendarController {
    private final RoomCalendarService service;

    public RoomCalendarController(RoomCalendarService service) {
        this.service = service;
    }

    @GetMapping("/room-calendar")
    public CalendarView calendar(@RequestParam(required = false) String month) {
        try {
            return service.calendar(month == null || month.isBlank() ? null : YearMonth.parse(month));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("month は YYYY-MM 形式で指定してください。");
        }
    }

    @GetMapping("/room-calendar/reservations/{id}")
    public ReservationDetail reservation(@PathVariable Integer id) {
        return service.reservationDetail(id);
    }

    @GetMapping("/room-calendar/blocks/{id}")
    public BlockDetail block(@PathVariable Integer id) {
        return service.blockDetail(id);
    }

    @PostMapping("/room-calendar/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomInventoryBlock createBlock(@RequestBody BlockWrite request, Principal principal) {
        return service.createBlock(request, actor(principal));
    }

    @PutMapping("/room-calendar/blocks/{id}")
    public RoomInventoryBlock updateBlock(@PathVariable Integer id, @RequestBody BlockUpdate request,
            Principal principal) {
        return service.updateBlock(id, request, actor(principal));
    }

    @PostMapping("/room-calendar/blocks/{id}/release")
    public ReleaseResult releaseBlock(@PathVariable Integer id, @RequestBody BlockRelease request,
            Principal principal) {
        return service.releaseBlock(id, request, actor(principal));
    }

    @PostMapping("/booking-requests/{id}/cancel")
    public Map<String, String> cancelBookingRequest(@PathVariable Integer id,
            @RequestBody CancelRequest request, Principal principal) {
        service.cancelBookingRequest(id, request.reason(), actor(principal));
        return Map.of("message", "予約申請を全室取消しました。");
    }

    @PostMapping("/rooms/{id}/cleaning-status")
    public Map<String, String> cleaningStatus(@PathVariable Integer id, @RequestBody CleaningRequest request) {
        service.updateCleaningStatus(id, request.cleaningStatus());
        return Map.of("message", "清掃状態を更新しました。");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(NoSuchElementException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler({InventoryConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(RuntimeException ex) {
        return Map.of("error", ex instanceof InventoryConflictException
                ? ex.getMessage()
                : "指定期間の在庫が変更されました。再読み込みしてください。");
    }

    private String actor(Principal principal) {
        return principal == null || principal.getName() == null ? "system" : principal.getName();
    }

    public record CancelRequest(String reason) {
    }
    public record CleaningRequest(String cleaningStatus) {
    }
}
