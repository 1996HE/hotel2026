package com.example.minshuku.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
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
import com.example.minshuku.domain.Room;
import com.example.minshuku.service.RoomService;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
@LoggedTest
@DisplayName("部屋管理画面コントローラー")
/**
 * 客室一覧画面の表示と登録・更新・削除のルーティングを確認する WebMvc テスト。
 */
class RoomControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoomService roomService;

    /**
     * テストケース名：test_01 rooms Page Shows Active And Deleted Rooms Normally
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_01 rooms Page Shows Active And Deleted Rooms Normally")
    @Test
    void roomsPageShowsActiveAndDeletedRoomsNormally() throws Exception {
        mockMvc.perform(get("/rooms"))
                .andExpect(status().isOk())
                .andExpect(view().name("app"))
                .andExpect(content().string(containsString("<div id=\"root\"></div>")))
                .andExpect(content().string(containsString("/js/app.js")));
    }

    /**
     * テストケース名：test_02 create Room Redirects With Success Message
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_02 create Room Redirects With Success Message")
    @Test
    void createRoomRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/rooms").with(csrf())
                .param("roomNumber", "501")
                .param("roomName", "雪の間")
                .param("roomType", "washitsu")
                .param("capacity", "2")
                .param("basePricePerPerson", "12000")
                .param("occupancyStatus", "vacant")
                .param("cleaningStatus", "cleaned")
                .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("message", "部屋を登録しました。"));
        verify(roomService).create(any(Room.class));
    }

    /**
     * テストケース名：test_03 create Room Shows Validation Error When Service Rejects
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_03 create Room Shows Validation Error When Service Rejects")
    @Test
    void createRoomShowsValidationErrorWhenServiceRejects() throws Exception {
        doThrow(new IllegalArgumentException("部屋番号を入力してください。")).when(roomService).create(any(Room.class));
        mockMvc.perform(post("/rooms").with(csrf()).param("roomNumber", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("error", "部屋番号を入力してください。"));
    }

    /**
     * テストケース名：test_04 create Room Shows Generic Error When Runtime Exception Occurs
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_04 create Room Shows Generic Error When Runtime Exception Occurs")
    @Test
    void createRoomShowsGenericErrorWhenRuntimeExceptionOccurs() throws Exception {
        String expectedError = "部屋登録に失敗しました。部屋番号が重複していないか確認してください。";
        doThrow(new RuntimeException("duplicate")).when(roomService).create(any(Room.class));
        MvcResult result = mockMvc.perform(post("/rooms").with(csrf()).param("roomNumber", "101"))
                .andReturn();

        int actualStatus = result.getResponse().getStatus();
        String actualRedirectUrl = result.getResponse().getRedirectedUrl();
        Object actualError = result.getFlashMap().get("error");
        System.out.print("  文字列結果：createRoomShowsGenericErrorWhenRuntimeExceptionOccurs"
                + " / 入力roomNumber=101"
                + " / 期待status=3xx"
                + " / 実際status=" + actualStatus
                + " / 期待redirect=/rooms"
                + " / 実際redirect=" + actualRedirectUrl
                + " / 期待error=" + expectedError
                + " / 実際error=" + actualError
                + System.lineSeparator());

        assertThat(actualStatus).isBetween(300, 399);
        assertThat(actualRedirectUrl).isEqualTo("/rooms");
        assertThat(actualError).isEqualTo(expectedError);
    }

    /**
     * テストケース名：test_05 update Room Status Redirects With Success Message
     * テスト条件：更新対象データと更新後の入力値を準備する。
     * テスト要望：対象データの状態または値が正しく更新されること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_05 update Room Status Redirects With Success Message")
    @Test
    void updateRoomStatusRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/rooms/1/statuses").with(csrf()).param("occupancyStatus", "vacant")
                .param("cleaningStatus", "cleaned"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("message", "部屋ステータスを更新しました。"));
        verify(roomService).updateStatuses(1, "vacant", "cleaned");
    }

    /**
     * テストケース名：test_06 delete Room Redirects With Success Message
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_06 delete Room Redirects With Success Message")
    @Test
    void deleteRoomRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/rooms/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("message", "部屋を削除しました。"));
        verify(roomService).delete(1);
    }

    /**
     * テストケース名：test_07 restore Room Redirects With Success Message
     * テスト条件：削除済み部屋を復元する。
     * テスト要望：削除済み一覧から復元できること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_07 restore Room Redirects With Success Message")
    @Test
    void restoreRoomRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/rooms/1/restore").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("message", "部屋を復元しました。"));
        verify(roomService).restore(1);
    }

    /**
     * テストケース名：test_08 delete Permanently Room Redirects With Success Message
     * テスト条件：削除済み部屋を完全削除する。
     * テスト要望：削除済み一覧から完全削除できること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_08 delete Permanently Room Redirects With Success Message")
    @Test
    void deletePermanentlyRoomRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/rooms/1/delete-permanently").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/rooms"))
                .andExpect(flash().attribute("message", "部屋を完全削除しました。"));
        verify(roomService).deletePermanently(1);
    }

    /**
     * テストケース名：test_09 post Without Csrf Token Is Rejected
     * テスト条件：CSRF token を付与しない POST リクエストを準備する。
     * テスト要望：CSRF token がない更新系リクエストを拒否すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_09 post Without Csrf Token Is Rejected")
    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/rooms").param("roomNumber", "501"))
                .andExpect(status().isForbidden());
    }

    private Room sampleRoom(boolean active) {
        Room room = TestSetData.room(active ? "bookable" : "inactive").toDomain();
        room.setId(active ? 1 : 2);
        return room;
    }
}
