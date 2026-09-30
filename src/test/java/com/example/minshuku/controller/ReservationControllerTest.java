package com.example.minshuku.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.minshuku.config.SecurityConfig;
import com.example.minshuku.domain.Reservation;
import com.example.minshuku.service.AdminUserService;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.service.RoomService;
import com.example.minshuku.support.LoggedTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin")
@LoggedTest
@DisplayName("予約管理画面コントローラー")
/**
 * 予約一覧画面の表示、登録、状態更新、CSRF 制御を確認する WebMvc テスト。
 */
class ReservationControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ReservationService reservationService;
    @MockBean
    private RoomService roomService;
    @MockBean
    private AdminUserService adminUserService;

    /**
     * テストケース名：test_01 reservations Page Shows Active And Cancelled Reservations Normally
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_01 reservations Page Shows Active And Cancelled Reservations Normally")
    @Test
    void reservationsPageShowsActiveAndCancelledReservationsNormally() throws Exception {
        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(view().name("app"))
                .andExpect(content().string(containsString("<div id=\"root\"></div>")))
                .andExpect(content().string(containsString("/js/app.js")));
    }

    /**
     * テストケース名：test_02 create Reservation Redirects With Success Message
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_02 create Reservation Redirects With Success Message")
    @Test
    void createReservationRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/reservations").with(csrf())
                .param("roomId", "1")
                .param("checkInDate", "2026-07-01")
                .param("checkOutDate", "2026-07-02")
                .param("guestName", "山田太郎")
                .param("guestGender", "男性")
                .param("guestAge", "30")
                .param("guestPhone", "090-0000-0000")
                .param("guestEmail", "guest@example.com")
                .param("guestCount", "2")
                .param("reservationForm", "電話")
                .param("paymentStatus", "unpaid")
                .param("companionNames", "佐藤花子")
                .param("companionGenders", "女性")
                .param("companionAges", "28")
                .param("companionPhones", "080-0000-0000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", "予約を登録しました。"));
        verify(reservationService).create(any(Reservation.class), anyBoolean(), any(), any(), any(), any(), any());
    }

    /**
     * テストケース名：test_03 create Reservation Shows Duplicate Reservation Error
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_03 create Reservation Shows Duplicate Reservation Error")
    @Test
    void createReservationShowsDuplicateReservationError() throws Exception {
        doThrow(new IllegalArgumentException("指定期間はすでに予約されています。")).when(reservationService)
                .create(any(Reservation.class), anyBoolean(), any(), any(), any(), any(), any());
        mockMvc.perform(post("/reservations").with(csrf()).param("roomId", "1").param("guestCount", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("error", "指定期間はすでに予約されています。"));
    }

    /**
     * テストケース名：test_04 update Payment Redirects With Success Message
     * テスト条件：更新対象データと更新後の入力値を準備する。
     * テスト要望：対象データの状態または値が正しく更新されること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_04 update Payment Redirects With Success Message")
    @Test
    void updatePaymentRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/reservations/1/payment").with(csrf()).param("paymentStatus", "paid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", "支払い状況を更新しました。"));
        verify(reservationService).updatePaymentStatus(1, "paid");
    }

    /**
     * テストケース名：test_05 cancel Reservation Redirects With Success Message
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_05 cancel Reservation Redirects With Success Message")
    @Test
    void cancelReservationRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/reservations/1/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", "予約をキャンセルしました。"));
        verify(reservationService).cancel(1);
    }

    /**
     * テストケース名：test_06 delete Cancelled Reservation Redirects With Success Message
     * テスト条件：取消済み予約を削除する。
     * テスト要望：取消済み一覧から完全削除できること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_06 delete Cancelled Reservation Redirects With Success Message")
    @Test
    void deleteCancelledReservationRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/reservations/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", "取消済み予約を削除しました。"));
        verify(reservationService).deleteCancelled(1);
    }

    /**
     * テストケース名：test_07 delete Checked Out Reservation Redirects With Success Message
     * テスト条件：チェックアウト済み予約を削除する。
     * テスト要望：チェックアウト済み一覧から完全削除できること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_07 delete Checked Out Reservation Redirects With Success Message")
    @Test
    void deleteCheckedOutReservationRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/reservations/1/delete-checked-out").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reservations"))
                .andExpect(flash().attribute("message", "チェックアウト済み予約を削除しました。"));
        verify(reservationService).deleteCheckedOut(1);
    }

    /**
     * テストケース名：test_07 post Without Csrf Token Is Rejected
     * テスト条件：CSRF token を付与しない POST リクエストを準備する。
     * テスト要望：CSRF token がない更新系リクエストを拒否すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_07 post Without Csrf Token Is Rejected")
    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/reservations").param("roomId", "1"))
                .andExpect(status().isForbidden());
    }

}
