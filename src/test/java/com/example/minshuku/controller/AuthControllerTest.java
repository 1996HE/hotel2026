package com.example.minshuku.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.minshuku.config.SecurityConfig;
import com.example.minshuku.service.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** 初回設定、セッションログイン、失敗記録、CSRF 保護を確認する。 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("管理者認証 API")
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockBean
    private AdminUserService adminUserService;

    @Test
    @DisplayName("未認証でも初回設定状態を取得できる")
    void statusIsPublic() throws Exception {
        when(adminUserService.setupRequired()).thenReturn(true);
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    @DisplayName("管理者初回設定は CSRF 付き JSON で実行できる")
    void setupCreatesAdministrator() throws Exception {
        mockMvc.perform(post("/api/auth/setup").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"owner\",\"password\":\"strong-pass-2026\"}"))
                .andExpect(status().isOk());
        verify(adminUserService).setup("owner", "strong-pass-2026");
    }

    @Test
    @DisplayName("正しいパスワードでセッションを開始する")
    void loginCreatesAuthenticatedSession() throws Exception {
        when(adminUserService.loadUserByUsername("owner"))
                .thenReturn(User.withUsername("owner")
                        .password(passwordEncoder.encode("strong-pass-2026"))
                        .roles("ADMIN")
                        .build());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"strong-pass-2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("ログインしました。"));
        verify(adminUserService).recordLoginSuccess("owner");
    }

    @Test
    @DisplayName("パスワード違いは同一メッセージで失敗回数を記録する")
    void loginFailureIsRecorded() throws Exception {
        when(adminUserService.loadUserByUsername("owner"))
                .thenReturn(User.withUsername("owner")
                        .password(passwordEncoder.encode("strong-pass-2026"))
                        .roles("ADMIN")
                        .build());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"owner\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ユーザー名またはパスワードが正しくありません。"));
        verify(adminUserService).recordLoginFailure("owner");
    }
}
