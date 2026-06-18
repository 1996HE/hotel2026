package com.example.minshuku.integration; // 実DBを使う予約サービステストの所属パッケージ。

import static org.assertj.core.api.Assertions.assertThat; // AssertJ の通常断言を使う。
import static org.assertj.core.api.Assertions.assertThatThrownBy; // AssertJ の例外断言を使う。

import com.example.minshuku.domain.Reservation; // 予約エンティティを使う。
import com.example.minshuku.domain.Room; // 部屋エンティティを使う。
import com.example.minshuku.mapper.ReservationMapper; // 予約 Mapper を使う。
import com.example.minshuku.mapper.RoomMapper; // 部屋 Mapper を使う。
import com.example.minshuku.service.ReservationService; // テスト対象の予約サービスを使う。
import java.math.BigDecimal; // 金額比較に使う。
import java.time.LocalDate; // 日付指定に使う。
import java.util.List; // 一覧比較に使う。
import org.junit.jupiter.api.BeforeEach; // テスト前準備に使う。
import org.junit.jupiter.api.Test; // テスト定義に使う。
import org.springframework.beans.factory.annotation.Autowired; // DI に使う。
import org.springframework.boot.test.context.SpringBootTest; // Spring 全体を起動する。
import org.springframework.transaction.annotation.Transactional; // 各テストをロールバックする。

@SpringBootTest // 実DB付きで Spring コンテキストを起動する。
@Transactional // 各テストの変更をロールバックする。
class ReservationServiceLocalDbTest extends LocalDbTestSupport { // 実DBで予約サービスを検証する。
  @Autowired private ReservationService reservationService; // テスト対象の予約サービスを注入する。
  @Autowired private ReservationMapper reservationMapper; // 結果確認用の予約 Mapper を注入する。
  @Autowired private RoomMapper roomMapper; // 結果確認用の部屋 Mapper を注入する。

  @BeforeEach // 各テストの前に実行する。
  void setUp() { // 初期データを準備する。
    resetTables(); // 既存データを消す。
    seedRooms(); // 部屋データを投入する。
    insertPriceRule(bookableRoomId, "9月料金", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), BigDecimal.valueOf(12000), 1, true, "9月のテスト料金"); // 予約計算用の料金ルールを投入する。
  }

  @Test // 正常系の予約登録を検証する。
  void createPersistsReservationAndCompanionsNormally() { // 予約と同行者が正しく保存されることを確認する。
    Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2); // 予約データを作る。
    reservation.setReservationForm("電話"); // 予約形式を設定する。
    reservation.setGuestEmail("guest@example.com"); // メールを設定する。
    reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000")); // 予約を登録する。
    Reservation saved = reservationMapper.findById(reservation.getId()); // 保存結果を取り出す。
    assertThat(saved.getReservationStatus()).isEqualTo("booked"); // 予約状態が有効であることを確認する。
    assertThat(saved.getPaymentStatus()).isEqualTo("unpaid"); // 支払い状態が未払いであることを確認する。
    assertThat(saved.getTotalAmount()).isEqualByComparingTo("48000"); // 料金ルール込みの金額計算を確認する。
    assertThat(roomMapper.findById(bookableRoomId).getOccupancyStatus()).isEqualTo("reserved"); // 部屋状態が予約済になることを確認する。
    assertThat(reservationService.findRecent()).hasSize(1); // 一覧検索で1件返ることを確認する。
    assertThat(reservationService.findRecent().get(0).getCompanionSummary()).contains("佐藤花子", "080-0000-0000"); // 同行者摘要に内容が入ることを確認する。
  }

  @Test // 異常系の空室条件違反を検証する。
  void createRejectsRoomThatIsNotVacant() { // 予約可能でない部屋は拒否される。
    Reservation reservation = baseReservation(occupiedRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2); // 予約データを作る。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 予約処理を実行して例外を確認する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("空室の部屋のみ予約できます。"); // 空室以外を拒否するメッセージを確認する。
  }

  @Test // 異常系の清掃条件違反を検証する。
  void createRejectsRoomThatIsNotCleaned() { // 清掃済みでない部屋は拒否される。
    Reservation reservation = baseReservation(dirtyRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2); // 予約データを作る。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 予約処理を実行して例外を確認する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("清掃済みの部屋のみ予約できます。"); // 清掃済み以外を拒否するメッセージを確認する。
  }

  @Test // 異常系の重複予約を検証する。
  void createRejectsDuplicateReservation() { // 同一部屋・同一期間の重複は拒否される。
    Reservation first = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2); // 1件目の予約を作る。
    reservationService.create(first, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000")); // 1件目を登録する。
    roomMapper.updateStatuses(bookableRoomId, "vacant", "cleaned"); // 重複判定を検証できるよう部屋状態だけを空室へ戻す。
    Reservation duplicate = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 13), 2); // 重複する2件目を作る。
    assertThatThrownBy(() -> reservationService.create(duplicate, false, List.of("鈴木花子"), List.of("スズキハナコ"), List.of("女性"), List.of(26), List.of("080-1111-1111"))) // 2件目を登録して例外を確認する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("指定期間はすでに予約されています。"); // 重複予約メッセージを確認する。
  }

  @Test // 異常系の入力形式違反を検証する。
  void createRejectsInvalidGuestPhone() { // 電話番号形式が不正なら拒否される。
    Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 1); // 1名予約データを作る。
    reservation.setGuestPhone("09000000000"); // 電話番号を不正形式にする。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of(), List.of(), List.of(), List.of(), List.of())) // 予約処理を実行して例外を確認する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("電話番号は000-0000-0000の形式で入力してください。"); // 電話番号形式エラーを確認する。
  }

  @Test // 正常系の自動チェックアウト同期を検証する。
  void syncDueCheckoutsMovesReservationToCheckedOutAndRoomToNeedsCleaning() { // 期限到来予約がチェックアウトへ進むことを確認する。
    Reservation reservation = baseReservation(spareRoomId, LocalDate.now().minusDays(2), LocalDate.now(), 2); // 今日チェックアウトの予約を作る。
    reservation.setGuestEmail("guest@example.com"); // メールを設定する。
    reservationService.create(reservation, false, List.of("同行者一"), List.of("ドウコウシャイチ"), List.of("男性"), List.of(31), List.of("090-1111-1111")); // 予約を登録する。
    reservationService.syncDueCheckouts(); // 自動チェックアウト同期を実行する。
    Reservation updated = reservationMapper.findById(reservation.getId()); // 更新後の予約を取り出す。
    Room room = roomMapper.findById(spareRoomId); // 対応部屋を取り出す。
    assertThat(updated.getReservationStatus()).isEqualTo("checked_out"); // チェックアウト状態に変わることを確認する。
    assertThat(room.getOccupancyStatus()).isEqualTo("vacant"); // 部屋が空室へ戻ることを確認する。
    assertThat(room.getCleaningStatus()).isEqualTo("needs_cleaning"); // 清掃待ちへ変わることを確認する。
  }

  @Test // 正常系の支払い更新を検証する。
  void updatePaymentStatusChangesStoredValue() { // 支払い状態だけが更新されることを確認する。
    Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2); // 予約を作る。
    reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000")); // 予約を登録する。
    reservationService.updatePaymentStatus(reservation.getId(), "paid"); // 支払い状態を更新する。
    assertThat(reservationMapper.findById(reservation.getId()).getPaymentStatus()).isEqualTo("paid"); // DB反映を確認する。
  }

  @Test // 正常系の予約取消を検証する。
  void cancelMovesReservationAndRoomBackToVacantCleaned() { // 取消時に部屋へ戻ることを確認する。
    Reservation reservation = baseReservation(bookableRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2); // 予約を作る。
    reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000")); // 予約を登録する。
    reservationService.cancel(reservation.getId()); // 取消処理を実行する。
    Reservation cancelled = reservationMapper.findById(reservation.getId()); // 取消後の予約を取り出す。
    Room room = roomMapper.findById(bookableRoomId); // 対応部屋を取り出す。
    assertThat(cancelled.getReservationStatus()).isEqualTo("cancelled"); // 取消状態を確認する。
    assertThat(room.getOccupancyStatus()).isEqualTo("vacant"); // 空室へ戻ることを確認する。
    assertThat(room.getCleaningStatus()).isEqualTo("cleaned"); // 清掃済へ戻ることを確認する。
  }

  @Test // 正常系のチェックアウト後清掃更新を検証する。
  void updateCheckoutCleaningStatusMovesRoomVacantWhenCleaned() { // 清掃完了で部屋を再予約可能に戻す。
    Reservation reservation = baseReservation(spareRoomId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12), 2); // 予約を作る。
    reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000")); // 予約を登録する。
    reservationService.updateReservationStatus(reservation.getId(), "checked_out"); // チェックアウト状態に切り替える。
    reservationService.updateCheckoutCleaningStatus(reservation.getId(), "cleaned"); // 清掃完了へ更新する。
    Room room = roomMapper.findById(spareRoomId); // 対応部屋を取り出す。
    assertThat(room.getOccupancyStatus()).isEqualTo("vacant"); // 空室へ戻ることを確認する。
    assertThat(room.getCleaningStatus()).isEqualTo("cleaned"); // 清掃済へ戻ることを確認する。
  }

  @Test // 異常系の予約不存在を検証する。
  void updateReservationStatusRejectsMissingReservation() { // 存在しない予約は拒否される。
    assertThatThrownBy(() -> reservationService.updateReservationStatus(9999, "booked")) // 存在しないIDで更新する。
      .isInstanceOf(IllegalArgumentException.class) // 業務例外であることを確認する。
      .hasMessage("予約が見つかりません。"); // 予約不存在メッセージを確認する。
  }

  private Reservation baseReservation(Integer roomId, LocalDate checkInDate, LocalDate checkOutDate, int guestCount) { // 予約入力の共通雛形を作る。
    Reservation reservation = new Reservation(); // 予約オブジェクトを作る。
    reservation.setRoomId(roomId); // 部屋IDを設定する。
    reservation.setCheckInDate(checkInDate); // チェックイン日を設定する。
    reservation.setCheckOutDate(checkOutDate); // チェックアウト日を設定する。
    reservation.setGuestName("山田太郎"); // 宿泊者名を設定する。
    reservation.setGuestKana("ヤマダタロウ"); // フリガナを設定する。
    reservation.setGuestGender("男性"); // 性別を設定する。
    reservation.setGuestAge(30); // 年齢を設定する。
    reservation.setGuestPhone("090-0000-0000"); // 電話番号を設定する。
    reservation.setGuestEmail("guest@example.com"); // メールを設定する。
    reservation.setGuestCount(guestCount); // 人数を設定する。
    reservation.setReservationForm("電話"); // 予約形式を設定する。
    reservation.setPaymentStatus("unpaid"); // 支払い状態を設定する。
    reservation.setNote("DBテスト用"); // メモを設定する。
    return reservation; // 予約雛形を返す。
  }
}
