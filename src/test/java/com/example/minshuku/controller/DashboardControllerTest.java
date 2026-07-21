package com.example.minshuku.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.minshuku.config.SecurityConfig;
import com.example.minshuku.service.ReservationService;
import com.example.minshuku.service.RoomService;
import com.example.minshuku.support.LoggedTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
@LoggedTest
@DisplayName("予約一覧画面コントローラー")
/**
 * ダッシュボードの表示項目とセキュリティヘッダーを確認する WebMvc テスト。
 */
class DashboardControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoomService roomService;
    @MockBean
    private ReservationService reservationService;

    /**
     * テストケース名：test_01 dashboard Shows Reservation List Normally
     * テスト条件：検索条件、初期データ、期待値を準備する。
     * テスト要望：取得結果が期待する一覧、件数、レスポンス内容と一致すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_01 dashboard Shows Reservation List Normally")
    @Test
    void dashboardShowsReservationListNormally() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("app"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow, noarchive"))
                .andExpect(content().string(containsString("<div id=\"root\"></div>")))
                .andExpect(content().string(containsString("/js/app.js")))
                .andExpect(content().string(containsString("白馬樹海")));
    }

    /**
     * テストケース名：test_02 root Path Shows Dashboard Normally
     * テスト条件：許可対象外または source-like path のアクセス条件を準備する。
     * テスト要望：不正または想定外のパスを安全に拒否すること。
     * テスト結果：期待値と実際値が一致すること。
     */
    @DisplayName("test_02 root Path Shows Dashboard Normally")
    @Test
    void rootPathShowsDashboardNormally() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("app"))
                .andExpect(content().string(containsString("<div id=\"root\"></div>")));
    }

    /**
     * テストケース名：test_03 source Like Paths Are Rejected
     * テスト条件：許可対象外または source-like path のアクセス条件を準備する。
     * テスト要望：不正または想定外のパスを安全に拒否すること。
     * テスト結果：処理結果が期待値と一致し、テストが成功すること。
     */
    @DisplayName("test_03 source Like Paths Are Rejected")
    @Test
    void sourceLikePathsAreRejected() throws Exception {
        mockMvc.perform(get("/src/main/java/com/example/minshuku/MinshukuManagementApplication.java"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/application.yml"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/styles/app.css.map"))
                .andExpect(status().isForbidden());
    }
}
