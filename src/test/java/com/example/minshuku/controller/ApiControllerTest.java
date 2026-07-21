package com.example.minshuku.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.minshuku.config.SecurityConfig;
import com.example.minshuku.domain.Reservation;
import com.example.minshuku.domain.Room;
import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.service.RoomPriceRuleService;
import com.example.minshuku.service.RoomService;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
@LoggedTest
@DisplayName("React API コントローラー")
/**
 * React フロントエンド向け JSON API の正常系、異常系、CSRF 制御を確認する。
 */
class ApiControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoomService roomService;
    @MockBean
    private ReservationService reservationService;
    @MockBean
    private RoomPriceRuleService priceRuleService;

    /**
     * テストケース名：test_01 dashboard Api Returns Summary And Reservations
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_01 dashboard Api Returns Summary And Reservations")
    @Test
    void dashboardApiReturnsSummaryAndReservations() throws Exception {
        when(roomService.countAll()).thenReturn(2);
        when(roomService.countVacant()).thenReturn(1);
        when(reservationService.countBooked()).thenReturn(1);
        when(reservationService.countRecent()).thenReturn(1);
        when(reservationService.findRecentPage(1, 5)).thenReturn(List.of(sampleReservation()));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCount").value(2))
                .andExpect(jsonPath("$.vacantCount").value(1))
                .andExpect(jsonPath("$.bookedCount").value(1))
                .andExpect(jsonPath("$.recentReservations.items[0].guestName").value("山田太郎"));

        verify(reservationService, never()).syncDueCheckouts();
    }

    /**
     * テストケース名：test_02 rooms Api Returns Active Deleted And Bookable Rooms
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_02 rooms Api Returns Active Deleted And Bookable Rooms")
    @Test
    void roomsApiReturnsActiveDeletedAndBookableRooms() throws Exception {
        when(roomService.findAll()).thenReturn(List.of(sampleRoom()));
        when(roomService.findInactive()).thenReturn(List.of(sampleRoom()));
        when(roomService.findBookable()).thenReturn(List.of(sampleRoom()));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rooms[0].roomNumber").value("101"))
                .andExpect(jsonPath("$.deletedRooms[0].roomName").value("桜の間"))
                .andExpect(jsonPath("$.bookableRooms[0].capacity").value(2));
    }

    /**
     * テストケース名：test_03 restore Room Api Returns Success Message
     * テスト条件：削除済み部屋の復元を行う。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_03 restore Room Api Returns Success Message")
    @Test
    void restoreRoomApiReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/rooms/1/restore").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("部屋を復元しました。"));
        verify(roomService).restore(1);
    }

    /**
     * テストケース名：test_04 delete Permanently Room Api Returns Success Message
     * テスト条件：削除済み部屋の完全削除を行う。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_04 delete Permanently Room Api Returns Success Message")
    @Test
    void deletePermanentlyRoomApiReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/rooms/1/delete-permanently").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("部屋を完全削除しました。"));
        verify(roomService).deletePermanently(1);
    }

    /**
     * テストケース名：test_05 delete Cancelled Reservation Api Returns Success Message
     * テスト条件：取消済み予約を削除する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_05 delete Cancelled Reservation Api Returns Success Message")
    @Test
    void deleteCancelledReservationApiReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/reservations/1/delete").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("取消済み予約を削除しました。"));
        verify(reservationService).deleteCancelled(1);
    }

    /**
     * テストケース名：test_06 delete Checked Out Reservation Api Returns Success Message
     * テスト条件：チェックアウト済み予約を削除する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_06 delete Checked Out Reservation Api Returns Success Message")
    @Test
    void deleteCheckedOutReservationApiReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/reservations/1/delete-checked-out").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("チェックアウト済み予約を削除しました。"));
        verify(reservationService).deleteCheckedOut(1);
    }

    /**
     * テストケース名：test_06 create Room Api Returns Success Message
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_06 create Room Api Returns Success Message")
    @Test
    void createRoomApiReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/rooms").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "roomNumber": "501",
                          "roomName": "雪の間",
                          "roomType": "washitsu",
                          "capacity": 2,
                          "basePricePerPerson": 12000,
                          "privateBath": false,
                          "occupancyStatus": "vacant",
                          "cleaningStatus": "cleaned",
                          "active": true
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("部屋を登録しました。"));
        verify(roomService).create(any(Room.class));
    }

    /**
     * テストケース名：test_07 create Room Api Returns Validation Error
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_07 create Room Api Returns Validation Error")
    @Test
    void createRoomApiReturnsValidationError() throws Exception {
        doThrow(new IllegalArgumentException("部屋番号を入力してください。")).when(roomService).create(any(Room.class));

        mockMvc.perform(post("/api/rooms").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomName\":\"雪の間\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("部屋番号を入力してください。"));
    }

    /**
     * テストケース名：test_08 prices Api Returns Rules And Rooms
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_08 prices Api Returns Rules And Rooms")
    @Test
    void pricesApiReturnsRulesAndRooms() throws Exception {
        when(priceRuleService.findAllWithRoom()).thenReturn(List.of(sampleRule()));
        when(roomService.findActive()).thenReturn(List.of(sampleRoom()));

        mockMvc.perform(get("/api/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules[0].ruleName").value("夏料金"))
                .andExpect(jsonPath("$.rooms[0].roomNumber").value("101"));
    }

    /**
     * テストケース名：test_09 reservations Api Returns Lists And Today
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_09 reservations Api Returns Lists And Today")
    @Test
    void reservationsApiReturnsListsAndToday() throws Exception {
        when(reservationService.countRecent()).thenReturn(28);
        when(reservationService.countCancelled()).thenReturn(8);
        when(reservationService.countCheckedOut()).thenReturn(11);
        when(reservationService.findRecentPage(2, 5)).thenReturn(List.of(sampleReservation()));
        when(reservationService.findCancelledPage(2, 5)).thenReturn(List.of(sampleReservation()));
        when(reservationService.findCheckedOutPage(3, 5)).thenReturn(List.of(sampleReservation()));
        when(roomService.findBookable()).thenReturn(List.of(sampleRoom()));
        when(reservationService.currentDate()).thenReturn(LocalDate.of(2026, 7, 6));

        mockMvc.perform(get("/api/reservations")
                        .param("page", "2")
                        .param("cancelledPage", "2")
                        .param("checkedOutPage", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservations.items[0].reservationStatusLabel").value("予約済"))
                .andExpect(jsonPath("$.reservations.page").value(2))
                .andExpect(jsonPath("$.reservations.totalPages").value(6))
                .andExpect(jsonPath("$.reservations.totalCount").value(28))
                .andExpect(jsonPath("$.cancelledReservations.page").value(2))
                .andExpect(jsonPath("$.cancelledReservations.totalCount").value(8))
                .andExpect(jsonPath("$.checkedOutReservations.page").value(3))
                .andExpect(jsonPath("$.checkedOutReservations.totalCount").value(11))
                .andExpect(jsonPath("$.rooms[0].roomNumber").value("101"))
                .andExpect(jsonPath("$.today").value("2026-07-06"));

        verify(reservationService).findRecentPage(2, 5);
        verify(reservationService).findCancelledPage(2, 5);
        verify(reservationService).findCheckedOutPage(3, 5);
        verify(reservationService, never()).syncDueCheckouts();
    }

    /**
     * テストケース名：test_10 reservations Api Clamps Page Numbers
     * テスト条件：0件、2ページ分、0件の各一覧に範囲外ページを指定する。
     * テスト要望：各ページ番号をそれぞれの最終ページへ正規化すること。
     * テスト結果：予約1、取消2、チェックアウト1ページとして検索・返却されること。
     */
    @DisplayName("test_10 reservations Api Clamps Page Numbers")
    @Test
    void reservationsApiClampsPageNumbers() throws Exception {
        when(reservationService.countRecent()).thenReturn(0);
        when(reservationService.countCancelled()).thenReturn(6);
        when(reservationService.countCheckedOut()).thenReturn(0);
        when(reservationService.findRecentPage(1, 5)).thenReturn(List.of());
        when(reservationService.findCancelledPage(2, 5)).thenReturn(List.of());
        when(reservationService.findCheckedOutPage(1, 5)).thenReturn(List.of());
        when(roomService.findBookable()).thenReturn(List.of());
        when(reservationService.currentDate()).thenReturn(LocalDate.of(2026, 7, 13));

        mockMvc.perform(get("/api/reservations")
                        .param("page", "-5")
                        .param("cancelledPage", "99")
                        .param("checkedOutPage", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservations.page").value(1))
                .andExpect(jsonPath("$.reservations.totalPages").value(1))
                .andExpect(jsonPath("$.reservations.totalCount").value(0))
                .andExpect(jsonPath("$.cancelledReservations.page").value(2))
                .andExpect(jsonPath("$.cancelledReservations.totalPages").value(2))
                .andExpect(jsonPath("$.checkedOutReservations.page").value(1));

        verify(reservationService).findRecentPage(1, 5);
        verify(reservationService).findCancelledPage(2, 5);
        verify(reservationService).findCheckedOutPage(1, 5);
    }

    /**
     * テストケース名：test_11 post Api Without Csrf Token Is Rejected
     * テスト条件：CSRF token を付与しない POST リクエストを準備する。
     * テスト要望：CSRF token がない更新系リクエストを拒否すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_11 post Api Without Csrf Token Is Rejected")
    @Test
    void postApiWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    private Reservation sampleReservation() {
        return TestSetData.reservation("paid").toDomain();
    }

    private Room sampleRoom() {
        Room room = TestSetData.room("bookable").toDomain();
        room.setId(1);
        return room;
    }

    private RoomPriceRule sampleRule() {
        return TestSetData.priceRule("summer").toDomain();
    }
}
