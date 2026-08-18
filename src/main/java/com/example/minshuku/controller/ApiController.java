package com.example.minshuku.controller;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.Room;
import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.service.RoomPriceRuleService;
import com.example.minshuku.service.RoomService;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.web.bind.annotation.PutMapping;

/**
 * React フロントエンドから利用する JSON API。
 * <p>
 * 画面描画に必要な参照データと、登録・更新系の業務操作を JSON で提供する。
 */
@RestController
@RequestMapping("/api")
public class ApiController {
    private static final int DASHBOARD_PAGE_SIZE = 5;
    private static final int RESERVATION_PAGE_SIZE = 5;

    private final RoomService roomService;
    private final ReservationService reservationService;
    private final RoomPriceRuleService priceRuleService;

    public ApiController(
            RoomService roomService,
            ReservationService reservationService,
            RoomPriceRuleService priceRuleService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.priceRuleService = priceRuleService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                roomService.countAll(),
                roomService.countVacant(),
                reservationService.countBooked(),
                firstPage(
                        reservationService.findRecentPage(1, DASHBOARD_PAGE_SIZE),
                        reservationService.countRecent(),
                        DASHBOARD_PAGE_SIZE));
    }

    @GetMapping("/rooms")
    public RoomsResponse rooms() {
        return new RoomsResponse(roomService.findAll(), roomService.findInactive(), roomService.findBookable());
    }

    @PostMapping("/rooms")
    public MessageResponse createRoom(@RequestBody Room room) {
        roomService.create(room);
        return new MessageResponse("部屋を登録しました。");
    }

    @PostMapping("/rooms/{id}/statuses")
    public MessageResponse updateRoomStatuses(@PathVariable Integer id, @RequestBody RoomStatusRequest request) {
        roomService.updateStatuses(id, request.occupancyStatus(), request.cleaningStatus());
        return new MessageResponse("部屋ステータスを更新しました。");
    }

    @PostMapping("/rooms/{id}/delete")
    public MessageResponse deleteRoom(@PathVariable Integer id) {
        roomService.delete(id);
        return new MessageResponse("部屋を削除しました。");
    }

    @PostMapping("/rooms/{id}/restore")
    public MessageResponse restoreRoom(@PathVariable Integer id) {
        roomService.restore(id);
        return new MessageResponse("部屋を復元しました。");
    }

    @PostMapping("/rooms/{id}/delete-permanently")
    public MessageResponse deleteRoomPermanently(@PathVariable Integer id) {
        roomService.deletePermanently(id);
        return new MessageResponse("部屋を完全削除しました。");
    }

    @GetMapping("/prices")
    public PricesResponse prices() {
        return new PricesResponse(priceRuleService.findAllWithRoom(), roomService.findActive());
    }

    @PostMapping("/prices")
    public MessageResponse createPriceRule(@RequestBody RoomPriceRule rule) {
        priceRuleService.create(rule);
        return new MessageResponse("料金ルールを登録しました。");
    }

    @PostMapping("/prices/{id}/delete")
    public MessageResponse deletePriceRule(@PathVariable Integer id) {
        priceRuleService.delete(id);
        return new MessageResponse("料金ルールを削除しました。");
    }

    @PostMapping("/prices/delete-selected")
    public MessageResponse deleteSelectedPriceRules(@RequestBody IdsRequest request) {
        priceRuleService.deleteByIds(request.ids());
        return new MessageResponse("料金ルールを削除しました。");
    }

    @GetMapping("/reservations")
    public ReservationsResponse reservations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "1") int cancelledPage,
            @RequestParam(defaultValue = "1") int checkedOutPage) {
        int reservationCount = reservationService.countRecent();
        int cancelledCount = reservationService.countCancelled();
        int checkedOutCount = reservationService.countCheckedOut();
        int safeReservationPage = safePage(page, reservationCount, RESERVATION_PAGE_SIZE);
        int safeCancelledPage = safePage(cancelledPage, cancelledCount, RESERVATION_PAGE_SIZE);
        int safeCheckedOutPage = safePage(checkedOutPage, checkedOutCount, RESERVATION_PAGE_SIZE);

        return new ReservationsResponse(
                page(
                        reservationService.findRecentPage(safeReservationPage, RESERVATION_PAGE_SIZE),
                        safeReservationPage,
                        reservationCount,
                        RESERVATION_PAGE_SIZE),
                page(
                        reservationService.findCancelledPage(safeCancelledPage, RESERVATION_PAGE_SIZE),
                        safeCancelledPage,
                        cancelledCount,
                        RESERVATION_PAGE_SIZE),
                page(
                        reservationService.findCheckedOutPage(safeCheckedOutPage, RESERVATION_PAGE_SIZE),
                        safeCheckedOutPage,
                        checkedOutCount,
                        RESERVATION_PAGE_SIZE),
                roomService.findBookable(),
                reservationService.currentDate());
    }

    @PostMapping("/reservations")
    public MessageResponse createReservation(@RequestBody ReservationCreateRequest request) {
        reservationService.create(
                request.reservation(),
                request.noPhoneInfo() && request.noEmailInfo(),
                request.companionNames(),
                request.companionKanas(),
                request.companionGenders(),
                request.companionAges(),
                request.companionPhones());
        return new MessageResponse("予約を登録しました。");
    }

    @PutMapping("/reservations/{id}")
    public MessageResponse updateReservation(@PathVariable Integer id, @RequestBody ReservationCreateRequest request) {
        reservationService.update(
                id,
                request.reservation(),
                request.noPhoneInfo() && request.noEmailInfo(),
                request.companionNames(),
                request.companionKanas(),
                request.companionGenders(),
                request.companionAges(),
                request.companionPhones());
        return new MessageResponse("予約を更新しました。");
    }

    @PostMapping("/reservations/{id}/check-in")
    public MessageResponse checkIn(@PathVariable Integer id) {
        reservationService.checkIn(id);
        return new MessageResponse("チェックインしました。");
    }

    @PostMapping("/reservations/{id}/check-out")
    public MessageResponse checkOut(@PathVariable Integer id) {
        reservationService.checkOut(id);
        return new MessageResponse("チェックアウトしました。");
    }

    @PostMapping("/reservations/{id}/payment")
    public MessageResponse updatePayment(@PathVariable Integer id, @RequestBody PaymentRequest request) {
        reservationService.updatePaymentStatus(id, request.paymentStatus());
        return new MessageResponse("支払い状況を更新しました。");
    }

    @PostMapping("/reservations/{id}/cancel")
    public MessageResponse cancelReservation(@PathVariable Integer id) {
        reservationService.cancel(id);
        return new MessageResponse("予約をキャンセルしました。");
    }

    @PostMapping("/reservations/{id}/delete")
    public MessageResponse deleteCancelledReservation(@PathVariable Integer id) {
        reservationService.deleteCancelled(id);
        return new MessageResponse("取消済み予約を削除しました。");
    }

    @PostMapping("/reservations/{id}/delete-checked-out")
    public MessageResponse deleteCheckedOutReservation(@PathVariable Integer id) {
        reservationService.deleteCheckedOut(id);
        return new MessageResponse("チェックアウト済み予約を削除しました。");
    }

    @PostMapping("/reservations/{id}/status")
    public MessageResponse updateReservationStatus(
            @PathVariable Integer id,
            @RequestBody ReservationStatusRequest request) {
        reservationService.updateReservationStatus(id, request.reservationStatus());
        return new MessageResponse("予約状態を更新しました。");
    }

    @PostMapping("/reservations/{id}/cleaning")
    public MessageResponse updateCleaning(@PathVariable Integer id, @RequestBody CleaningRequest request) {
        reservationService.updateCheckoutCleaningStatus(id, request.cleaningStatus());
        return new MessageResponse("清掃状態を更新しました。");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        // サービス層の業務エラーを、React が表示しやすい JSON エラーへ変換する。
        return new ErrorResponse(ex.getMessage());
    }

    private <T> PageResponse<T> firstPage(List<T> items, int totalCount, int pageSize) {
        return page(items, 1, totalCount, pageSize);
    }

    private <T> PageResponse<T> page(List<T> items, int page, int totalCount, int pageSize) {
        return new PageResponse<>(items, page, totalPages(totalCount, pageSize), totalCount);
    }

    private int safePage(int requestedPage, int totalCount, int pageSize) {
        return Math.min(Math.max(1, requestedPage), totalPages(totalCount, pageSize));
    }

    private int totalPages(int totalCount, int pageSize) {
        // データが0件でも画面側のページ表示を1ページとして扱う。
        return Math.max(1, (totalCount + pageSize - 1) / pageSize);
    }

    /**
     * ダッシュボード初期表示で必要な集計値と直近予約一覧。
     */
    public record DashboardResponse(int roomCount, int vacantCount, int bookedCount,
            PageResponse<Reservation> recentReservations) {
    }

    /**
     * 一覧系レスポンス共通のページ情報。
     */
    public record PageResponse<T>(List<T> items, int page, int totalPages, int totalCount) {
    }

    /**
     * 客室管理画面で使う有効客室、削除済み客室、予約可能客室。
     */
    public record RoomsResponse(List<Room> rooms, List<Room> deletedRooms, List<Room> bookableRooms) {
    }

    /**
     * 料金管理画面で使う料金ルール一覧と登録対象客室。
     */
    public record PricesResponse(List<RoomPriceRule> rules, List<Room> rooms) {
    }

    /**
     * 予約管理画面で使う予約一覧、取消一覧、チェックアウト一覧、予約可能客室、業務日付。
     */
    public record ReservationsResponse(
            PageResponse<Reservation> reservations,
            PageResponse<Reservation> cancelledReservations,
            PageResponse<Reservation> checkedOutReservations,
            List<Room> rooms,
            LocalDate today) {
    }

    /**
     * 客室の宿泊状態と清掃状態を同時更新するためのリクエスト。
     */
    public record RoomStatusRequest(String occupancyStatus, String cleaningStatus) {
    }

    /**
     * 一括削除対象の ID 一覧。
     */
    public record IdsRequest(List<Integer> ids) {
    }

    /**
     * 予約本体と同行者配列をまとめて登録するためのリクエスト。
     */
    public record ReservationCreateRequest(
            Reservation reservation,
            boolean noPhoneInfo,
            boolean noEmailInfo,
            List<String> companionNames,
            List<String> companionKanas,
            List<String> companionGenders,
            List<Integer> companionAges,
            List<String> companionPhones) {
    }

    /**
     * 支払い状態更新リクエスト。
     */
    public record PaymentRequest(String paymentStatus) {
    }

    /**
     * 予約状態更新リクエスト。
     */
    public record ReservationStatusRequest(String reservationStatus) {
    }

    /**
     * チェックアウト後の清掃状態更新リクエスト。
     */
    public record CleaningRequest(String cleaningStatus) {
    }

    /**
     * 更新系 API の正常終了メッセージ。
     */
    public record MessageResponse(String message) {
    }

    /**
     * 業務エラーを画面へ返すレスポンス。
     */
    public record ErrorResponse(String error) {
    }
}
