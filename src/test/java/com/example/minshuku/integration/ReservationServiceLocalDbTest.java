package com.example.minshuku.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.Room;
import com.example.minshuku.mapper.ReservationMapper;
import com.example.minshuku.mapper.RoomMapper;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@LoggedTest
@DisplayName("予約サービスDB連携")
/**
 * 予約業務のDB連携、番号発番、状態遷移、料金計算を確認する結合テスト。
 */
class ReservationServiceLocalDbTest extends LocalDbTestSupport {
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private RoomMapper roomMapper;

    @BeforeEach
    void setUp() {
        resetTables();
        seedRooms();
        var priceRule = TestSetData.priceRule("september");
        insertPriceRule(bookableRoomId, priceRule.ruleName(), priceRule.startDate(), priceRule.endDate(),
                priceRule.pricePerPerson(), priceRule.priority(), priceRule.active(), priceRule.note());
    }

    /**
     * テストケース名：test_01 create Persists Reservation And Companions Normally
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_01 create Persists Reservation And Companions Normally")
    @Test
    void createPersistsReservationAndCompanionsNormally() {
        Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                2);
        reservation.setReservationForm("電話");
        reservation.setGuestEmail("guest@example.com");
        reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28),
                List.of("080-0000-0000"));
        Reservation saved = reservationMapper.findById(reservation.getId());
        assertThat(saved.getReservationNo()).isEqualTo("R000001");
        assertThat(saved.getReservationNo()).hasSize(7);
        assertThat(saved.getReservationStatus()).isEqualTo("booked");
        assertThat(saved.getPaymentStatus()).isEqualTo("unpaid");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("48000");
        assertThat(roomMapper.findById(bookableRoomId).getOccupancyStatus()).isEqualTo("vacant");
        assertThat(reservationService.findRecent()).hasSize(1);
        assertThat(reservationService.findRecent().get(0).getCompanionSummary()).contains("佐藤花子", "080-0000-0000");
    }

    /**
     * テストケース名：test_02 create Generates Compact Reservation No In Reservation Order
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_02 create Generates Compact Reservation No In Reservation Order")
    @Test
    void createGeneratesCompactReservationNoInReservationOrder() {
        Reservation first = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11), 1);
        reservationService.create(first, false, List.of(), List.of(), List.of(), List.of(), List.of());

        Reservation second = baseReservation(spareRoomId, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 12), 1);
        reservationService.create(second, false, List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(reservationMapper.findById(first.getId()).getReservationNo()).isEqualTo("R000001");
        assertThat(reservationMapper.findById(second.getId()).getReservationNo()).isEqualTo("R000002");
    }

    /**
     * テストケース名：test_03 create Does Not Cycle Reservation No After Legacy Upper Limit
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_03 create Does Not Cycle Reservation No After Legacy Upper Limit")
    @Test
    void createDoesNotCycleReservationNoAfterLegacyUpperLimit() {
        jdbcTemplate.execute("ALTER SEQUENCE reservation_no_seq RESTART WITH 99999");

        Reservation last = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11), 1);
        reservationService.create(last, false, List.of(), List.of(), List.of(), List.of(), List.of());

        Reservation next = baseReservation(spareRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11), 1);
        reservationService.create(next, false, List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(reservationMapper.findById(last.getId()).getReservationNo()).isEqualTo("R099999");
        assertThat(reservationMapper.findById(next.getId()).getReservationNo()).isEqualTo("R100000");
    }

    /**
     * テストケース名：test_04 create Rejects Room That Is Not Vacant
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_04 create Rejects Room That Is Not Vacant")
    @Test
    void createRejectsRoomThatIsNotVacant() {
        LocalDate today = reservationService.currentDate();
        Reservation reservation = baseReservation(occupiedRoomId, today, today.plusDays(2), 2);
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("当日予約は空室の部屋のみ登録できます。");
    }

    /**
     * テストケース名：test_05 create Rejects Room That Is Not Cleaned
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_05 create Rejects Room That Is Not Cleaned")
    @Test
    void createRejectsRoomThatIsNotCleaned() {
        LocalDate today = reservationService.currentDate();
        Reservation reservation = baseReservation(dirtyRoomId, today, today.plusDays(2), 2);
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"),
                List.of("女性"), List.of(28), List.of("080-0000-0000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("当日予約は清掃済みの部屋のみ登録できます。");
    }

    /**
     * テストケース名：test_06 create Rejects Duplicate Reservation
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_06 create Rejects Duplicate Reservation")
    @Test
    void createRejectsDuplicateReservation() {
        Reservation first = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2);
        reservationService.create(first, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28),
                List.of("080-0000-0000"));
        roomMapper.updateStatuses(bookableRoomId, "vacant", "cleaned");
        Reservation duplicate = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 13),
                2);
        assertThatThrownBy(() -> reservationService.create(duplicate, false, List.of("鈴木花子"), List.of("スズキハナコ"),
                List.of("女性"), List.of(26), List.of("080-1111-1111")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("指定期間はすでに予約されています。");
    }

    /**
     * テストケース名：test_07 create Rejects Invalid Guest Phone
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_07 create Rejects Invalid Guest Phone")
    @Test
    void createRejectsInvalidGuestPhone() {
        Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                1);
        reservation.setGuestPhone("09000000000");
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of(), List.of(), List.of(),
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("電話番号は000-0000-0000の形式で入力してください。");
    }

    /**
     * テストケース名：test_08 create Rejects Guest Count Over Ten
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_08 create Rejects Guest Count Over Ten")
    @Test
    void createRejectsGuestCountOverTen() {
        Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12), 11);
        assertThatThrownBy(() -> reservationService.create(reservation, false, List.of(), List.of(), List.of(),
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("宿泊人数は10名以下にしてください。");
    }

    /**
     * テストケース名：test_09 sync Due Checkouts Moves Reservation To Checked Out And Room To Needs Cleaning
     * テスト条件：対象処理に必要な入力値、mock、または DB 状態を準備する。
     * テスト要望：対象処理の期待仕様を満たすこと。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_09 sync Due Checkouts Moves Reservation To Checked Out And Room To Needs Cleaning")
    @Test
    void syncDueCheckoutsMovesReservationToCheckedOutAndRoomToNeedsCleaning() {
        LocalDate systemDate = reservationService.currentDate();
        int reservationId = insertReservation(spareRoomId, "R000001", systemDate.minusDays(2), systemDate,
                "山田太郎", "ヤマダタロウ", "男性", 30, "090-0000-0000", "guest@example.com", 2, "公式", "unpaid",
                "booked", java.math.BigDecimal.valueOf(24000), "チェックアウト同期テスト");
        insertReservationGuest(reservationId, "同行者一", "ドウコウシャイチ", "男性", 31, "090-1111-1111");
        roomMapper.updateStatuses(spareRoomId, "reserved", "cleaned");
        reservationService.syncDueCheckouts();
        Reservation updated = reservationMapper.findById(reservationId);
        Room room = roomMapper.findById(spareRoomId);
        assertThat(updated.getReservationStatus()).isEqualTo("checked_out");
        assertThat(room.getOccupancyStatus()).isEqualTo("vacant");
        assertThat(room.getCleaningStatus()).isEqualTo("needs_cleaning");
    }

    /**
     * テストケース名：test_10 update Payment Status Changes Stored Value
     * テスト条件：更新対象データと更新後の入力値を準備する。
     * テスト要望：対象データの状態または値が正しく更新されること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_10 update Payment Status Changes Stored Value")
    @Test
    void updatePaymentStatusChangesStoredValue() {
        Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                2);
        reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28),
                List.of("080-0000-0000"));
        reservationService.updatePaymentStatus(reservation.getId(), "paid");
        assertThat(reservationMapper.findById(reservation.getId()).getPaymentStatus()).isEqualTo("paid");
    }

    /**
     * テストケース名：test_11 cancel Moves Reservation And Room Back To Vacant Cleaned
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_11 cancel Moves Reservation And Room Back To Vacant Cleaned")
    @Test
    void cancelMovesReservationAndRoomBackToVacantCleaned() {
        Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                2);
        reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28),
                List.of("080-0000-0000"));
        reservationService.cancel(reservation.getId());
        Reservation cancelled = reservationMapper.findById(reservation.getId());
        Room room = roomMapper.findById(bookableRoomId);
        assertThat(cancelled.getReservationStatus()).isEqualTo("cancelled");
        assertThat(room.getOccupancyStatus()).isEqualTo("vacant");
        assertThat(room.getCleaningStatus()).isEqualTo("cleaned");
    }

    /**
     * テストケース名：test_12 delete Cancelled Reservation Removes Row Normally
     * テスト条件：取消済み予約を準備する。
     * テスト要望：取消済み予約を一覧から削除できること。
     * テスト結果：DB から行が消えること。
     */
    @DisplayName("test_12 delete Cancelled Reservation Removes Row Normally")
    @Test
    void deleteCancelledReservationRemovesRowNormally() {
        Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                2);
        reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28),
                List.of("080-0000-0000"));
        reservationService.cancel(reservation.getId());
        reservationService.deleteCancelled(reservation.getId());
        assertThat(reservationMapper.findById(reservation.getId())).isNull();
        assertThat(reservationService.findCancelled()).isEmpty();
    }

    /**
     * テストケース名：test_13 delete Checked Out Reservation Removes Row Normally
     * テスト条件：チェックアウト済み予約を準備する。
     * テスト要望：チェックアウト済み予約を一覧から削除できること。
     * テスト結果：DB から行が消えること。
     */
    @DisplayName("test_13 delete Checked Out Reservation Removes Row Normally")
    @Test
    void deleteCheckedOutReservationRemovesRowNormally() {
        LocalDate today = reservationService.currentDate();
        int reservationId = insertReservation(ruleRoomId, "R000001", today.minusDays(3), today.minusDays(1),
                "山田太郎", "ヤマダタロウ", "男性", 30, "090-0000-0000", "guest@example.com", 2, "公式", "paid",
                "checked_out", java.math.BigDecimal.valueOf(24000), "削除確認");
        reservationService.deleteCheckedOut(reservationId);
        assertThat(reservationMapper.findById(reservationId)).isNull();
        assertThat(reservationService.countCheckedOut()).isEqualTo(0);
    }

    /**
     * テストケース名：test_13 update Checkout Cleaning Status Moves Room Vacant When Cleaned
     * テスト条件：更新対象データと更新後の入力値を準備する。
     * テスト要望：対象データの状態または値が正しく更新されること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_13 update Checkout Cleaning Status Moves Room Vacant When Cleaned")
    @Test
    void updateCheckoutCleaningStatusMovesRoomVacantWhenCleaned() {
        LocalDate today = reservationService.currentDate();
        Reservation reservation = baseReservation(spareRoomId, today, today.plusDays(2), 2);
        reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28),
                List.of("080-0000-0000"));
        reservationService.updateReservationStatus(reservation.getId(), "checked_out");
        reservationService.updateCheckoutCleaningStatus(reservation.getId(), "cleaned");
        Room room = roomMapper.findById(spareRoomId);
        assertThat(room.getOccupancyStatus()).isEqualTo("vacant");
        assertThat(room.getCleaningStatus()).isEqualTo("cleaned");
    }

    /**
     * テストケース名：test_14 update Reservation Status Rejects Missing Reservation
     * テスト条件：更新対象データと更新後の入力値を準備する。
     * テスト要望：対象データの状態または値が正しく更新されること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_14 update Reservation Status Rejects Missing Reservation")
    @Test
    void updateReservationStatusRejectsMissingReservation() {
        assertThatThrownBy(() -> reservationService.updateReservationStatus(9999, "booked"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("予約が見つかりません。");
    }

    /**
     * テストケース名：test_15 query Reservation Pages Compares Expected And Actual Results
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_15 query Reservation Pages Compares Expected And Actual Results")
    @Test
    void queryReservationPagesComparesExpectedAndActualResults() {
        Reservation booked = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 1);
        reservationService.create(booked, false, List.of(), List.of(), List.of(), List.of(), List.of());

        Reservation cancelled = baseReservation(spareRoomId, LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 14), 1);
        reservationService.create(cancelled, false, List.of(), List.of(), List.of(), List.of(), List.of());
        reservationService.cancel(cancelled.getId());

        LocalDate today = reservationService.currentDate();
        Reservation checkedOut = baseReservation(ruleRoomId, today, today.plusDays(1), 1);
        reservationService.create(checkedOut, false, List.of(), List.of(), List.of(), List.of(), List.of());
        reservationService.updateReservationStatus(checkedOut.getId(), "checked_out");

        printComparison("正常系検索：有効予約件数", 1, reservationService.countRecent());
        assertThat(reservationService.countRecent()).isEqualTo(1);
        printComparison("正常系検索：取消予約件数", 1, reservationService.countCancelled());
        assertThat(reservationService.countCancelled()).isEqualTo(1);
        printComparison("正常系検索：チェックアウト件数", 1, reservationService.countCheckedOut());
        assertThat(reservationService.countCheckedOut()).isEqualTo(1);

        List<String> actualRecentStatuses = reservationService.findRecentPage(1, 5).stream()
                .map(Reservation::getReservationStatus)
                .toList();
        printComparison("正常系検索：有効予約ステータス一覧", List.of("booked"), actualRecentStatuses);
        assertThat(actualRecentStatuses).containsExactly("booked");

        List<String> actualCancelledStatuses = reservationService.findCancelledPage(1, 5).stream()
                .map(Reservation::getReservationStatus)
                .toList();
        printComparison("正常系検索：取消予約ステータス一覧", List.of("cancelled"), actualCancelledStatuses);
        assertThat(actualCancelledStatuses).containsExactly("cancelled");

        List<String> actualCheckedOutStatuses = reservationService.findCheckedOutPage(1, 5).stream()
                .map(Reservation::getReservationStatus)
                .toList();
        printComparison("正常系検索：チェックアウトステータス一覧", List.of("checked_out"), actualCheckedOutStatuses);
        assertThat(actualCheckedOutStatuses).containsExactly("checked_out");
    }

    /**
     * テストケース名：test_15 query Reservation Page Returns Empty For Out Of Range Data
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_15 query Reservation Page Returns Empty For Out Of Range Data")
    @Test
    void queryReservationPageReturnsEmptyForOutOfRangeData() {
        Reservation booked = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 1);
        reservationService.create(booked, false, List.of(), List.of(), List.of(), List.of(), List.of());

        List<String> actualStatuses = reservationService.findRecentPage(99, 5).stream()
                .map(Reservation::getReservationStatus)
                .toList();
        printComparison("範囲外データ：有効予約99ページ", List.of(), actualStatuses);
        assertThat(actualStatuses).isEmpty();
    }

    /**
     * テストケース名：test_16 create Reservation Rejects Guest Count Over Capacity As Abnormal Case
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_16 create Reservation Rejects Guest Count Over Capacity As Abnormal Case")
    @Test
    void createReservationRejectsGuestCountOverCapacityAsAbnormalCase() {
        Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                3);
        String actualMessage = null;
        try {
            reservationService.create(
                    reservation,
                    false,
                    List.of("佐藤花子", "田中一郎"),
                    List.of("サトウハナコ", "タナカイチロウ"),
                    List.of("女性", "男性"),
                    List.of(28, 31),
                    List.of("080-0000-0000", "080-1111-1111"));
        } catch (IllegalArgumentException ex) {
            actualMessage = ex.getMessage();
        }
        printComparison("異常系：宿泊人数が定員超過", "宿泊人数が部屋の定員を超えています。", actualMessage);
        assertThat(actualMessage).isEqualTo("宿泊人数が部屋の定員を超えています。");
    }

    @DisplayName("test_17 create Allows Non Overlapping Future Reservations For Same Room")
    @Test
    void createAllowsNonOverlappingFutureReservationsForSameRoom() {
        Reservation first = baseReservation(
                bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 1);
        reservationService.create(first, false, List.of(), List.of(), List.of(), List.of(), List.of());

        Reservation second = baseReservation(
                bookableRoomId, LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 14), 1);
        reservationService.create(second, false, List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(reservationService.countBooked()).isEqualTo(2);
        assertThat(roomMapper.findById(bookableRoomId).getOccupancyStatus()).isEqualTo("vacant");
    }

    @DisplayName("test_18 sync Due Checkouts Keeps Room Reserved For Same Day Following Stay")
    @Test
    void syncDueCheckoutsKeepsRoomReservedForSameDayFollowingStay() {
        LocalDate today = reservationService.currentDate();
        int dueReservationId = insertReservation(
                spareRoomId,
                "R000001",
                today.minusDays(2),
                today,
                "山田太郎",
                "ヤマダタロウ",
                "男性",
                30,
                "090-0000-0000",
                "first@example.com",
                1,
                "公式",
                "paid",
                "booked",
                java.math.BigDecimal.valueOf(12000),
                "退房対象");
        insertReservation(
                spareRoomId,
                "R000002",
                today,
                today.plusDays(1),
                "佐藤花子",
                "サトウハナコ",
                "女性",
                28,
                "080-0000-0000",
                "second@example.com",
                1,
                "公式",
                "paid",
                "booked",
                java.math.BigDecimal.valueOf(12000),
                "当日後続予約");
        roomMapper.updateStatuses(spareRoomId, "reserved", "cleaned");

        reservationService.syncDueCheckouts();

        assertThat(reservationMapper.findById(dueReservationId).getReservationStatus()).isEqualTo("checked_out");
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
        reservation.setNote("DBテスト用");
        return reservation;
    }
}
