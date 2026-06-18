package com.example.minshuku.controller; // 宣言予約ページ测试所属パッケージ。

import static org.hamcrest.Matchers.containsString; // 読み込み字符串パッケージ含断言工具。
import static org.mockito.ArgumentMatchers.any; // 読み込み Mockito 任意パラメータ匹配器。
import static org.mockito.ArgumentMatchers.anyBoolean; // 読み込み Mockito 真偽値匹配器。
import static org.mockito.Mockito.doThrow; // 読み込み Mockito 异常行に设定メソッド。
import static org.mockito.Mockito.verify; // 読み込み Mockito 呼び出し検証メソッド。
import static org.mockito.Mockito.when; // 読み込み Mockito 行に设定メソッド。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // 読み込み GET リクエスト构造メソッド。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // 読み込み POST リクエスト构造メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // 読み込みレスポンスコンテンツ断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash; // 読み込み flash 断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model; // 読み込みモデル断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl; // 読み込みリダイレクト URL 断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // 読み込み HTTP 状態断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view; // 読み込み視图名称断言メソッド。

import com.example.minshuku.domain.Reservation; // 読み込み予約エンティティ用构造ページデータ。
import com.example.minshuku.domain.Room; // 読み込み部屋エンティティ用构造ページデータ。
import com.example.minshuku.service.ReservationService; // 読み込み被 mock の予約サービス。
import com.example.minshuku.service.RoomService; // 読み込み被 mock の部屋サービス。
import java.time.LocalDate; // 読み込み日付型用测试データ。
import java.util.List; // 読み込み一覧型用测试データ。
import org.junit.jupiter.api.Test; // 読み込み JUnit 测试アノテーション。
import org.springframework.beans.factory.annotation.Autowired; // 読み込み测试依赖注入アノテーション。
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // 読み込み MVC 切片测试アノテーション。
import org.springframework.boot.test.mock.mockito.MockBean; // 読み込み Spring Boot mock bean アノテーション。
import org.springframework.test.web.servlet.MockMvc; // 読み込み MockMvc 测试客户端。

@WebMvcTest(ReservationController.class) // のみ加载予約コントローラーと MVC 相关组件。
class ReservationControllerTest { // 定義予約ページコントローラー测试。
  @Autowired private MockMvc mockMvc; // 注入 MockMvc 用模拟 HTTP リクエスト。
  @MockBean private ReservationService reservationService; // 注入予約サービス mock。
  @MockBean private RoomService roomService; // 注入部屋サービス mock。

  @Test // 標记正常テーブル示予約管理ページの测试。
  void reservationsPageShowsActiveAndCancelledReservationsNormally() throws Exception { // 测试予約ページ正常渲染。
    when(reservationService.findRecentPage(1, 5)).thenReturn(List.of(sampleReservation("booked"))); // 准备有効予約一覧。
    when(reservationService.findCancelledPage(1, 5)).thenReturn(List.of(sampleReservation("cancelled"))); // 准备取消予約一覧。
    when(reservationService.findCheckedOutPage(1, 5)).thenReturn(List.of(sampleReservation("checked_out"))); // 准备退房予約一覧。
    when(reservationService.countRecent()).thenReturn(1); // 准备有効予約总数。
    when(reservationService.countCancelled()).thenReturn(1); // 准备取消予約总数。
    when(reservationService.countCheckedOut()).thenReturn(1); // 准备退房予約总数。
    when(roomService.findBookable()).thenReturn(List.of(sampleRoom())); // 准备可能予約部屋一覧。
    mockMvc.perform(get("/reservations")) // リクエスト予約管理ページ。
      .andExpect(status().isOk()) // 断言ページレスポンス成功。
      .andExpect(view().name("reservations")) // 断言返却 reservations 模板。
      .andExpect(model().attributeExists("reservations", "cancelledReservations", "rooms", "reservation")) // 断言モデルデータ齐全。
      .andExpect(content().string(containsString("予約一覧"))) // 断言有効予約一覧標題テーブル示。
      .andExpect(content().string(containsString("取消予約一覧"))) // 断言取消予約一覧標題テーブル示。
      .andExpect(content().string(containsString("チェックアウト一覧"))); // 断言チェックアウト一覧標題テーブル示。
  }

  @Test // 標记予約登録正常パス测试。
  void createReservationRedirectsWithSuccessMessage() throws Exception { // 测试予約登録成功。
    mockMvc.perform(post("/reservations") // 送信予約登録フォーム。
        .param("roomId", "1") // 設定部屋番号。
        .param("checkInDate", "2026-07-01") // 設定宿泊日付。
        .param("checkOutDate", "2026-07-02") // 設定退房日付。
        .param("guestName", "山田太郎") // 設定予約客户姓名。
        .param("guestGender", "男性") // 設定予約客户性别。
        .param("guestAge", "30") // 設定予約客户年龄。
        .param("guestPhone", "090-0000-0000") // 設定电话。
        .param("guestEmail", "guest@example.com") // 設定邮箱。
        .param("guestCount", "2") // 設定人数。
        .param("reservationForm", "電話") // 設定予約形式。
        .param("paymentStatus", "unpaid") // 設定付款状態。
        .param("companionNames", "佐藤花子") // 設定同行者姓名。
        .param("companionGenders", "女性") // 設定同行者性别。
        .param("companionAges", "28") // 設定同行者年龄。
        .param("companionPhones", "080-0000-0000")) // 設定同行者电话。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/reservations")) // 断言リダイレクトへ予約ページ。
      .andExpect(flash().attribute("message", "予約を登録しました。")); // 断言成功メッセージ。
    verify(reservationService).create(any(Reservation.class), anyBoolean(), any(), any(), any(), any(), any()); // 検証呼び出し予約登録サービス。
  }

  @Test // 標记重複予約エラーパス测试。
  void createReservationShowsDuplicateReservationError() throws Exception { // 测试重複予約时テーブル示エラーメッセージ。
    doThrow(new IllegalArgumentException("指定期間はすでに予約されています。")).when(reservationService).create(any(Reservation.class), anyBoolean(), any(), any(), any(), any(), any()); // 准备重複予約异常。
    mockMvc.perform(post("/reservations").param("roomId", "1").param("guestCount", "2")) // 送信会被サービス拒绝の予約。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/reservations")) // 断言リダイレクトへ予約ページ。
      .andExpect(flash().attribute("error", "指定期間はすでに予約されています。")); // 断言重複予約エラーメッセージ。
  }

  @Test // 標记付款状態更新正常パス测试。
  void updatePaymentRedirectsWithSuccessMessage() throws Exception { // 测试付款状態更新成功。
    mockMvc.perform(post("/reservations/1/payment").param("paymentStatus", "paid")) // 送信付款状態更新。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/reservations")) // 断言リダイレクトへ予約ページ。
      .andExpect(flash().attribute("message", "支払い状況を更新しました。")); // 断言成功メッセージ。
    verify(reservationService).updatePaymentStatus(1, "paid"); // 検証呼び出し付款状態更新サービス。
  }

  @Test // 標记取消予約正常パス测试。
  void cancelReservationRedirectsWithSuccessMessage() throws Exception { // 测试取消予約成功。
    mockMvc.perform(post("/reservations/1/cancel")) // 送信取消予約リクエスト。
      .andExpect(status().is3xxRedirection()) // 断言发生リダイレクト。
      .andExpect(redirectedUrl("/reservations")) // 断言リダイレクトへ予約ページ。
      .andExpect(flash().attribute("message", "予約をキャンセルしました。")); // 断言成功メッセージ。
    verify(reservationService).cancel(1); // 検証呼び出し取消予約サービス。
  }

  private Reservation sampleReservation(String status) { // 定義构造予約测试データのメソッド。
    Reservation reservation = new Reservation(); // 作成予約オブジェクト。
    reservation.setReservationNo("RSV-" + status); // 設定予約番号。
    reservation.setRoomNumber("101"); // 設定部屋番号。
    reservation.setRoomName("桜の間"); // 設定部屋名称。
    reservation.setGuestName("山田太郎"); // 設定予約客户姓名。
    reservation.setGuestGender("男性"); // 設定予約客户性别。
    reservation.setGuestAge(30); // 設定予約客户年龄。
    reservation.setGuestPhone("090-0000-0000"); // 設定予約客户电话。
    reservation.setGuestEmail("guest@example.com"); // 設定予約客户邮箱。
    reservation.setGuestCount(2); // 設定人数。
    reservation.setReservationForm("電話"); // 設定予約形式。
    reservation.setCompanionSummary("佐藤花子（女性・28歳・080-0000-0000）"); // 設定同行者摘要。
    reservation.setCheckInDate(LocalDate.of(2026, 7, 1)); // 設定宿泊日付。
    reservation.setCheckOutDate(LocalDate.of(2026, 7, 2)); // 設定退房日付。
    reservation.setPaymentStatus("unpaid"); // 設定付款状態。
    reservation.setReservationStatus(status); // 設定予約状態。
    return reservation; // 返却予約オブジェクト。
  }

  private Room sampleRoom() { // 定義构造可能予約部屋测试データのメソッド。
    Room room = new Room(); // 作成部屋オブジェクト。
    room.setId(1); // 設定部屋主キー。
    room.setRoomNumber("101"); // 設定部屋番号。
    room.setRoomName("桜の間"); // 設定部屋名称。
    room.setCapacity(2); // 設定定員。
    return room; // 返却部屋オブジェクト。
  }
}
