package com.example.minshuku.controller;

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
import com.example.minshuku.domain.RoomPriceRule;
import com.example.minshuku.service.AdminUserService;
import com.example.minshuku.service.RoomPriceRuleService;
import com.example.minshuku.service.RoomService;
import com.example.minshuku.support.LoggedTest;
import com.example.minshuku.support.TestSetData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PriceRuleController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin")
@LoggedTest
@DisplayName("料金管理画面コントローラー")
/**
 * 料金ルール画面の表示と単体削除・一括削除のルーティングを確認する WebMvc テスト。
 */
class PriceRuleControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoomPriceRuleService priceRuleService;
    @MockBean
    private RoomService roomService;
    @MockBean
    private AdminUserService adminUserService;

    /**
     * テストケース名：test_01 prices Page Shows Rules Normally
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_01 prices Page Shows Rules Normally")
    @Test
    void pricesPageShowsRulesNormally() throws Exception {
        mockMvc.perform(get("/prices"))
                .andExpect(status().isOk())
                .andExpect(view().name("app"))
                .andExpect(content().string(containsString("<div id=\"root\"></div>")))
                .andExpect(content().string(containsString("/js/app.js")));
    }

    /**
     * テストケース名：test_02 create Price Rule Redirects With Success Message
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_02 create Price Rule Redirects With Success Message")
    @Test
    void createPriceRuleRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/prices").with(csrf())
                .param("roomId", "1")
                .param("ruleName", "夏料金")
                .param("startDate", "2026-07-01")
                .param("endDate", "2026-08-31")
                .param("pricePerPerson", "12000")
                .param("priority", "1")
                .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prices"))
                .andExpect(flash().attribute("message", "料金ルールを登録しました。"));
        verify(priceRuleService).create(any(RoomPriceRule.class));
    }

    /**
     * テストケース名：test_03 create Price Rule Shows Validation Error When Service Rejects
     * テスト条件：登録対象データ、関連 mock、または DB 初期データを準備する。
     * テスト要望：正常入力は保存・遷移・レスポンスが成功し、不正入力は業務エラーになること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_03 create Price Rule Shows Validation Error When Service Rejects")
    @Test
    void createPriceRuleShowsValidationErrorWhenServiceRejects() throws Exception {
        doThrow(new IllegalArgumentException("開始日は終了日以前にしてください。")).when(priceRuleService)
                .create(any(RoomPriceRule.class));
        mockMvc.perform(post("/prices").with(csrf()).param("roomId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prices"))
                .andExpect(flash().attribute("error", "開始日は終了日以前にしてください。"));
    }

    /**
     * テストケース名：test_04 delete Price Rule Redirects With Success Message
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_04 delete Price Rule Redirects With Success Message")
    @Test
    void deletePriceRuleRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/prices/10/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prices"))
                .andExpect(flash().attribute("message", "料金ルールを削除しました。"));
        verify(priceRuleService).delete(10);
    }

    /**
     * テストケース名：test_05 delete Price Rule Also Works On Legacy Path
     * テスト条件：許可対象外または source-like path のアクセス条件を準備する。
     * テスト要望：不正または想定外のパスを安全に拒否すること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_05 delete Price Rule Also Works On Legacy Path")
    @Test
    void deletePriceRuleAlsoWorksOnLegacyPath() throws Exception {
        mockMvc.perform(post("/prices/10").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prices"))
                .andExpect(flash().attribute("message", "料金ルールを削除しました。"));
        verify(priceRuleService).delete(10);
    }

    /**
     * テストケース名：test_06 delete Selected Price Rules Redirects With Success Message
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：期待したリダイレクト先と flash message になること。
     */
    @DisplayName("test_06 delete Selected Price Rules Redirects With Success Message")
    @Test
    void deleteSelectedPriceRulesRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/prices/delete-selected").with(csrf())
                .param("ids", "10", "11"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prices"))
                .andExpect(flash().attribute("message", "料金ルールを削除しました。"));
        verify(priceRuleService).deleteByIds(List.of(10, 11));
    }

    /**
     * テストケース名：test_07 delete Selected Price Rules Shows Validation Error When Nothing Selected
     * テスト条件：削除または取消対象データを準備する。
     * テスト要望：対象データが削除・取消済みとして正しく処理されること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_07 delete Selected Price Rules Shows Validation Error When Nothing Selected")
    @Test
    void deleteSelectedPriceRulesShowsValidationErrorWhenNothingSelected() throws Exception {
        doThrow(new IllegalArgumentException("料金ルールを1件以上選択してください。")).when(priceRuleService).deleteByIds(any());
        mockMvc.perform(post("/prices/delete-selected").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prices"))
                .andExpect(flash().attribute("error", "料金ルールを1件以上選択してください。"));
    }

    /**
     * テストケース名：test_08 post Without Csrf Token Is Rejected
     * テスト条件：CSRF token を付与しない POST リクエストを準備する。
     * テスト要望：CSRF token がない更新系リクエストを拒否すること。
     * テスト結果：期待したエラー、拒否結果、または空結果になること。
     */
    @DisplayName("test_08 post Without Csrf Token Is Rejected")
    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/prices").param("roomId", "1"))
                .andExpect(status().isForbidden());
    }

    private RoomPriceRule sampleRule() {
        return TestSetData.priceRule("summer").toDomain();
    }

    private Room sampleRoom() {
        Room room = TestSetData.room("bookable").toDomain();
        room.setId(1);
        return room;
    }
}
