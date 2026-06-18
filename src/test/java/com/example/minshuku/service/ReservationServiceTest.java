package com.example.minshuku.service; // 宣言予約サービス测试所属パッケージ。

import static org.assertj.core.api.Assertions.assertThat; // 読み込み AssertJ 普通断言工具。
import static org.assertj.core.api.Assertions.assertThatThrownBy; // 読み込み AssertJ 异常断言工具。
import static org.mockito.ArgumentMatchers.any; // 読み込み Mockito 任意パラメータ匹配器。
import static org.mockito.Mockito.never; // 読み込み Mockito 未呼び出し検証工具。
import static org.mockito.Mockito.verify; // 読み込み Mockito 呼び出し検証工具。
import static org.mockito.Mockito.when; // 読み込み Mockito 行に设定工具。

import com.example.minshuku.domain.Reservation; // 読み込み予約エンティティ用构造测试入力。
import com.example.minshuku.domain.ReservationGuest; // 読み込み同行者エンティティ用検証保存コンテンツ。
import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ用构造部屋状態。
import com.example.minshuku.mapper.ReservationGuestMapper; // 読み込み同行者 Mapper mock 型。
import com.example.minshuku.mapper.ReservationMapper; // 読み込み予約 Mapper mock 型。
import com.example.minshuku.mapper.RoomMapper; // 読み込み部屋 Mapper mock 型。
import com.example.minshuku.mapper.RoomPriceRuleMapper; // 読み込み料金ルール Mapper mock 型。
import java.math.BigDecimal; // 読み込み金額型用設定部屋基本料金。
import java.time.LocalDate; // 読み込み日付型用設定宿泊期间。
import java.util.List; // 読み込み一覧型用同行者パラメータ。
import org.junit.jupiter.api.BeforeEach; // 読み込み测试前置処理アノテーション。
import org.junit.jupiter.api.Test; // 読み込み JUnit 测试アノテーション。
import org.junit.jupiter.api.extension.ExtendWith; // 読み込み JUnit 扩展アノテーション。
import org.mockito.ArgumentCaptor; // 読み込み Mockito パラメータ捕获器。
import org.mockito.Mock; // 読み込み Mockito mock アノテーション。
import org.mockito.junit.jupiter.MockitoExtension; // 読み込み Mockito JUnit 扩展。

@ExtendWith(MockitoExtension.class) // 启用 Mockito mock 初始化。
class ReservationServiceTest { // 予約サービス業務テストを定義。
  @Mock private ReservationMapper reservationMapper; // 作成予約 Mapper mock。
  @Mock private ReservationGuestMapper reservationGuestMapper; // 作成同行者 Mapper mock。
  @Mock private RoomMapper roomMapper; // 作成部屋 Mapper mock。
  @Mock private RoomPriceRuleMapper priceRuleMapper; // 作成料金ルール Mapper mock。
  private ReservationService reservationService; // 保存被测试の予約サービス实例。

  @BeforeEach // 標记每个测试执行前运行。
  void setUp() { // 定義测试前置処理。
    reservationService = new ReservationService(reservationMapper, reservationGuestMapper, roomMapper, priceRuleMapper); // 用 mock 依赖作成サービス实例。
  }

  @Test // 標记正常予約登録测试。
  void createRegistersReservationAndCompanionNormally() { // 测试空室且清掃済部屋可能以成功予約。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    Room room = sampleBookableRoom(); // 构造可能予約部屋。
    when(roomMapper.findById(1)).thenReturn(room); // 准备部屋検索返却值。
    when(reservationMapper.countOverlapping(1, reservation.getCheckInDate(), reservation.getCheckOutDate())).thenReturn(0); // 准备無重複予約結果。
    reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000")); // 执行予約登録。
    ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class); // 作成予約パラメータ捕获器。
    ArgumentCaptor<ReservationGuest> guestCaptor = ArgumentCaptor.forClass(ReservationGuest.class); // 作成同行者パラメータ捕获器。
    verify(reservationMapper).insert(reservationCaptor.capture()); // 検証予約记录被書き込みと捕获コンテンツ。
    verify(reservationGuestMapper).insert(guestCaptor.capture()); // 検証同行者记录被書き込みと捕获コンテンツ。
    verify(roomMapper).updateStatuses(1, "reserved", "cleaned"); // 検証部屋状態被改に予約済。
    assertThat(reservationCaptor.getValue().getReservationStatus()).isEqualTo("booked"); // 断言予約状態に完了予約。
    assertThat(reservationCaptor.getValue().getReservationStatusLabel()).isEqualTo("予約済"); // 断言予約状態標签可能直接用画面テーブル示。
    assertThat(reservationCaptor.getValue().getPaymentStatus()).isEqualTo("unpaid"); // 断言缺省付款状態に未付款。
    assertThat(reservationCaptor.getValue().getPaymentStatusLabel()).isEqualTo("未払い"); // 断言付款状態標签可能直接用画面テーブル示。
    assertThat(reservationCaptor.getValue().getReservationForm()).isEqualTo("公式"); // 断言缺省予約形式に公式。
    assertThat(reservationCaptor.getValue().getTotalAmount()).isEqualByComparingTo("24000"); // 断言一晚两人の金額計算正确。
    assertThat(guestCaptor.getValue().getGuestName()).isEqualTo("佐藤花子"); // 断言同行者姓名保存正确。
    assertThat(guestCaptor.getValue().getGuestKana()).isEqualTo("サトウハナコ"); // 断言同行者假名保存正确。
    assertThat(guestCaptor.getValue().getGuestGender()).isEqualTo("女性"); // 断言同行者性别保存正确。
    assertThat(guestCaptor.getValue().getGuestAge()).isEqualTo(28); // 断言同行者年龄保存正确。
    assertThat(guestCaptor.getValue().getGuestPhone()).isEqualTo("080-0000-0000"); // 断言同行者电话保存正确。
  }

  @Test // 標记重複予約エラー测试。
  void createRejectsDuplicateReservationForSameRoomAndDate() { // 测试同部屋日付重叠时非可能重複予約。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    when(roomMapper.findById(1)).thenReturn(sampleBookableRoom()); // 准备可能予約部屋返却值。
    when(reservationMapper.countOverlapping(1, reservation.getCheckInDate(), reservation.getCheckOutDate())).thenReturn(1); // 准备完了有重叠予約結果。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 执行と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型に業務検証异常。
      .hasMessage("指定期間はすでに予約されています。"); // 断言メッセージに重複予約。
    verify(reservationMapper, never()).insert(any(Reservation.class)); // 断言重複予約非会書き込み予約テーブル。
    verify(reservationGuestMapper, never()).insert(any(ReservationGuest.class)); // 断言重複予約非会書き込み同行者テーブル。
    verify(roomMapper, never()).updateStatuses(any(), any(), any()); // 断言重複予約非会更新部屋状態。
  }

  @Test // 標记非空室エラー测试。
  void createRejectsRoomThatIsNotVacant() { // 测试非是空室の部屋非可能予約。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    Room room = sampleBookableRoom(); // 构造基本部屋。
    room.setOccupancyStatus("occupied"); // 設定部屋に使用中。
    when(roomMapper.findById(1)).thenReturn(room); // 准备部屋検索返却值。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 执行と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型に業務検証异常。
      .hasMessage("空室の部屋のみ予約できます。"); // 断言メッセージにのみ允许空室予約。
    verify(reservationMapper, never()).countOverlapping(any(), any(), any()); // 断言部屋状態非合格时非继续查重複。
    verify(reservationMapper, never()).insert(any(Reservation.class)); // 断言非会書き込み予約テーブル。
  }

  @Test // 標记未清掃エラー测试。
  void createRejectsRoomThatIsNotCleaned() { // 测试空室但未清掃の部屋非可能予約。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    Room room = sampleBookableRoom(); // 构造基本部屋。
    room.setCleaningStatus("needs_cleaning"); // 設定部屋に未清掃。
    when(roomMapper.findById(1)).thenReturn(room); // 准备部屋検索返却值。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 执行と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型に業務検証异常。
      .hasMessage("清掃済みの部屋のみ予約できます。"); // 断言メッセージにのみ允许清掃済部屋予約。
    verify(reservationMapper, never()).countOverlapping(any(), any(), any()); // 断言清掃状態非合格时非继续查重複。
    verify(reservationMapper, never()).insert(any(Reservation.class)); // 断言非会書き込み予約テーブル。
  }

  @Test // 標记予約人フリガナ格式エラー测试。
  void createRejectsInvalidGuestKana() { // 测试予約人フリガナ非是全角片假名时拒绝。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    reservation.setGuestKana("やまだたろう"); // 設定に平假名，模拟エラー入力。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 执行と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型に業務検証异常。
      .hasMessage("フリガナは全角カタカナで入力してください。"); // 断言メッセージにフリガナ格式エラー。
    verify(roomMapper, never()).findById(any()); // 断言で基本検証失败时非会検索部屋。
  }

  @Test // 標记予約人电话格式エラー测试。
  void createRejectsInvalidGuestPhone() { // 测试予約人电话パッケージ含非法字符时拒绝。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    reservation.setGuestPhone("090-0000-0000x"); // 設定非法电话字符。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 执行と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型に業務検証异常。
      .hasMessage("電話番号は000-0000-0000の形式で入力してください。"); // 断言メッセージに电话格式エラー。
    verify(roomMapper, never()).findById(any()); // 断言で基本検証失败时非会検索部屋。
  }

  @Test // 標记予約人メール格式エラー测试。
  void createRejectsInvalidGuestEmail() { // 测试予約人邮件格式非正确时拒绝。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    reservation.setGuestEmail("guest.example.com"); // 設定非法邮件地址。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000"))) // 执行と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型に業務検証异常。
      .hasMessage("メールアドレスの形式が正しくありません。"); // 断言メッセージに邮件格式エラー。
    verify(roomMapper, never()).findById(any()); // 断言で基本検証失败时非会検索部屋。
  }

  @Test // 標记同行者电话格式エラー测试。
  void createRejectsInvalidCompanionPhone() { // 测试同行者电话格式非正确时拒绝。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    assertThatThrownBy(() -> reservationService.create(reservation, false, List.of("佐藤花子"), List.of("サトウハナコ"), List.of("女性"), List.of(28), List.of("080-0000-0000x"))) // 执行と断言异常。
      .isInstanceOf(IllegalArgumentException.class) // 断言异常型に業務検証异常。
      .hasMessage("電話番号は000-0000-0000の形式で入力してください。"); // 断言メッセージに电话格式エラー。
    verify(roomMapper, never()).findById(any()); // 断言で同行者検証失败时非会検索部屋。
  }

  @Test // 標记无电话无邮件正常予約测试。
  void createAllowsReservationWithoutPhoneAndEmailWhenNoContactInfoIsSelected() { // 测试勾选无联系方式时可登録。
    Reservation reservation = sampleReservation(); // 构造有効予約入力。
    reservation.setGuestCount(1); // 設定連絡先なしの単独予約。
    reservation.setGuestPhone(null); // 取消电话输入。
    reservation.setGuestEmail(null); // 取消邮箱输入。
    Room room = sampleBookableRoom(); // 构造可能予約部屋。
    when(roomMapper.findById(1)).thenReturn(room); // 准备部屋検索返却值。
    when(reservationMapper.countOverlapping(1, reservation.getCheckInDate(), reservation.getCheckOutDate())).thenReturn(0); // 准备無重複予約結果。
    reservationService.create(reservation, true, List.of(), List.of(), List.of(), List.of(), List.of()); // 执行无联系方式予約登録。
    verify(reservationMapper).insert(any(Reservation.class)); // 検証予約が保存される。
    verify(roomMapper).updateStatuses(1, "reserved", "cleaned"); // 検証部屋状態被更新。
  }

  private Reservation sampleReservation() { // 定義构造予約入力の辅助メソッド。
    Reservation reservation = new Reservation(); // 作成予約オブジェクト。
    reservation.setRoomId(1); // 設定予約部屋番号。
    reservation.setCheckInDate(LocalDate.of(2026, 7, 1)); // 設定宿泊日付。
    reservation.setCheckOutDate(LocalDate.of(2026, 7, 2)); // 設定退房日付。
    reservation.setGuestName("山田太郎"); // 設定予約客户姓名。
    reservation.setGuestGender("男性"); // 設定予約客户性别。
    reservation.setGuestAge(30); // 設定予約客户年龄。
    reservation.setGuestPhone("090-0000-0000"); // 設定予約客户电话。
    reservation.setGuestCount(2); // 設定予約人数。
    return reservation; // 返却予約オブジェクト。
  }

  private Room sampleBookableRoom() { // 定義构造可能予約部屋の辅助メソッド。
    Room room = new Room(); // 作成部屋オブジェクト。
    room.setId(1); // 設定部屋主キー。
    room.setRoomNumber("101"); // 設定部屋番号。
    room.setRoomName("桜の間"); // 設定部屋名称。
    room.setCapacity(2); // 設定部屋定員。
    room.setBasePricePerPerson(BigDecimal.valueOf(12000)); // 設定每人基本単価。
    room.setOccupancyStatus("vacant"); // 設定部屋に空室。
    room.setCleaningStatus("cleaned"); // 設定部屋に清掃済。
    room.setActive(true); // 設定部屋启用。
    return room; // 返却部屋オブジェクト。
  }

  @Test // 標记予約状態標签测试。
  void reservationLabelsReflectStatusCodes() { // 测试ドメインエンティティの状態標签由状態码正确派生。
    Reservation reservation = new Reservation(); // 作成予約オブジェクト。
    reservation.setReservationStatus("cancelled"); // 設定取消状態码。
    reservation.setPaymentStatus("paid"); // 設定完了支付状態码。
    assertThat(reservation.getReservationStatusLabel()).isEqualTo("取消済"); // 断言予約状態標签に取消済。
    assertThat(reservation.getPaymentStatusLabel()).isEqualTo("支払済"); // 断言支付状態標签に支払済。
  }
}
