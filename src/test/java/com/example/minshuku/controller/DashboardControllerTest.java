package com.example.minshuku.controller; // 宣言予約一覧ページテスト所属パッケージ。

import static org.hamcrest.Matchers.containsString; // 読み込み字符串パッケージ含断言工具。
import static org.mockito.Mockito.when; // 読み込み Mockito 行に设定メソッド。
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // 読み込み GET リクエスト構築メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // 読み込みレスポンスコンテンツ断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model; // 読み込みモデル断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // 読み込み HTTP 状態断言メソッド。
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view; // 読み込み視图名称断言メソッド。

import com.example.minshuku.domain.Reservation; // 読み込み予約エンティティ用構築ページデータ。
import com.example.minshuku.service.ReservationService; // 読み込み被 mock の予約サービス。
import com.example.minshuku.service.RoomService; // 読み込み被 mock の部屋サービス。
import java.time.LocalDate; // 読み込み日付型用テストデータ。
import java.util.List; // 読み込み一覧型用テストデータ。
import org.junit.jupiter.api.Test; // 読み込み JUnit テストアノテーション。
import org.springframework.beans.factory.annotation.Autowired; // 読み込みテスト依赖注入アノテーション。
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // 読み込み MVC 切り出しテストアノテーション。
import org.springframework.boot.test.mock.mockito.MockBean; // 読み込み Spring Boot mock bean アノテーション。
import org.springframework.test.web.servlet.MockMvc; // 読み込み MockMvc テスト宿泊者端。

@WebMvcTest(DashboardController.class) // のみ加载予約一覧コントローラーと MVC 相关组件。
class DashboardControllerTest { // 定義予約一覧ページコントローラーテスト。
  @Autowired private MockMvc mockMvc; // 注入 MockMvc 用模拟 HTTP リクエスト。
  @MockBean private RoomService roomService; // 注入部屋サービス mock。
  @MockBean private ReservationService reservationService; // 注入予約サービス mock。

  @Test // 標记正常表示予約一覧ページのテスト。
  void dashboardShowsReservationListNormally() throws Exception { // テスト正常情况下予約一覧ページ可能渲染。
    when(roomService.countAll()).thenReturn(2); // 准备部屋総数。
    when(roomService.countVacant()).thenReturn(1); // 准备空室件数。
    when(reservationService.countBooked()).thenReturn(1); // 准备有効予約件数。
    when(reservationService.countRecent()).thenReturn(1); // 准备予約総数。
    when(reservationService.findRecentPage(1, 5)).thenReturn(List.of(sampleReservation())); // 准备予約一覧データ。
    mockMvc.perform(get("/dashboard")) // リクエスト予約一覧ページ。
      .andExpect(status().isOk()) // 断言ページレスポンス成功。
      .andExpect(view().name("dashboard")) // 断言返却 dashboard 模板。
      .andExpect(model().attribute("roomCount", 2)) // 断言部屋総数モデル值。
      .andExpect(model().attribute("vacantCount", 1)) // 断言空室件数モデル值。
      .andExpect(model().attribute("bookedCount", 1)) // 断言有効予約件数モデル值。
      .andExpect(model().attribute("reservationPage", 1)) // 断言現在ページ番号モデル值。
      .andExpect(model().attribute("reservationTotalPages", 1)) // 断言总页数モデル值。
      .andExpect(content().string(containsString("白馬樹海 予約一覧"))) // 断言ページ標題存で。
      .andExpect(content().string(containsString("山田太郎"))); // 断言予約宿泊者表示でページ。
  }

  @Test // 標记根パス正常遷移へ同一予約一覧模板のテスト。
  void rootPathShowsDashboardNormally() throws Exception { // テスト根パス也能表示予約一覧。
    when(reservationService.countRecent()).thenReturn(0); // 准备予約総数に零。
    when(reservationService.findRecentPage(1, 5)).thenReturn(List.of()); // 准备空予約一覧。
    mockMvc.perform(get("/")) // リクエスト根パス。
      .andExpect(status().isOk()) // 断言ページレスポンス成功。
      .andExpect(view().name("dashboard")) // 断言返却 dashboard 模板。
      .andExpect(content().string(containsString("予約データがありません。"))); // 断言空一覧メッセージ表示。
  }

  private Reservation sampleReservation() { // 定義構築予約一覧テストデータのメソッド。
    Reservation reservation = new Reservation(); // 作成予約オブジェクト。
    reservation.setReservationNo("RSV-TEST"); // 設定予約番号。
    reservation.setRoomNumber("101"); // 設定部屋番号。
    reservation.setRoomName("桜の間"); // 設定部屋名称。
    reservation.setGuestName("山田太郎"); // 設定予約宿泊者氏名。
    reservation.setGuestGender("男性"); // 設定予約宿泊者性別。
    reservation.setGuestAge(30); // 設定予約宿泊者年齢。
    reservation.setGuestPhone("090-0000-0000"); // 設定予約宿泊者電話。
    reservation.setGuestEmail("guest@example.com"); // 設定予約宿泊者メール。
    reservation.setGuestCount(2); // 設定予約宿泊者人数。
    reservation.setReservationForm("電話"); // 設定予約形式。
    reservation.setCompanionSummary("佐藤花子（女性・28歳・080-0000-0000）"); // 設定同行者摘要。
    reservation.setCheckInDate(LocalDate.of(2026, 7, 1)); // 設定宿泊日付。
    reservation.setCheckOutDate(LocalDate.of(2026, 7, 2)); // 設定チェックアウト日付。
    reservation.setPaymentStatus("paid"); // 設定支払い状態。
    reservation.setReservationStatus("booked"); // 設定予約状態。
    return reservation; // 返却予約オブジェクト。
  }
}
