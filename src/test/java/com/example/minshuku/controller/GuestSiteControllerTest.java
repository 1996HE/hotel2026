package com.example.minshuku.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.minshuku.service.AdminUserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GuestSiteController.class)
@Import(com.example.minshuku.config.SecurityConfig.class)
class GuestSiteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @ParameterizedTest
    @ValueSource(strings = {"/stay", "/stay/rooms", "/stay/rates", "/stay/guide", "/stay/access", "/stay/ski",
            "/stay/reserve", "/stay/booking", "/stay/cancel"})
    void guestPagesArePublic(String path) throws Exception {
        mockMvc.perform(get(path).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(view().name("guest"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("guest-root")));
    }
}
