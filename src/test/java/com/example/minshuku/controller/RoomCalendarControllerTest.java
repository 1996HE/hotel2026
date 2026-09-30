package com.example.minshuku.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.minshuku.config.SecurityConfig;
import com.example.minshuku.domain.RoomInventoryBlock;
import com.example.minshuku.service.AdminUserService;
import com.example.minshuku.service.InventoryConflictException;
import com.example.minshuku.service.RoomCalendarService;
import com.example.minshuku.service.RoomCalendarService.BlockWrite;
import com.example.minshuku.service.RoomCalendarService.CalendarView;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomCalendarController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin")
class RoomCalendarControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RoomCalendarService service;
    @MockBean
    private AdminUserService adminUserService;

    @Test
    @WithAnonymousUser
    void calendarApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/room-calendar").param("month", "2026-10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void calendarReturnsNaturalMonthProjection() throws Exception {
        YearMonth month = YearMonth.of(2026, 10);
        when(service.calendar(month)).thenReturn(new CalendarView(
                "2026-10",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 9, 30),
                OffsetDateTime.parse("2026-09-30T12:00:00+09:00"),
                List.of(),
                List.of(),
                List.of()));

        mockMvc.perform(get("/api/room-calendar").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.monthStart").value("2026-10-01"))
                .andExpect(jsonPath("$.nextMonthStart").value("2026-11-01"));
    }

    @Test
    void blockWriteRequiresCsrf() throws Exception {
        mockMvc.perform(post("/api/room-calendar/blocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "roomId": 1,
                          "blockType": "maintenance",
                          "startDate": "2026-10-01",
                          "endDateExclusive": "2026-10-02",
                          "reason": "点検"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void blockWriteReturnsCreatedAndActor() throws Exception {
        RoomInventoryBlock block = new RoomInventoryBlock();
        block.setId(9);
        block.setRoomId(1);
        block.setBlockType("maintenance");
        block.setStartDate(LocalDate.of(2026, 10, 1));
        block.setEndDateExclusive(LocalDate.of(2026, 10, 2));
        block.setReason("点検");
        block.setVersion(0L);
        when(service.createBlock(any(BlockWrite.class), eq("admin"))).thenReturn(block);

        mockMvc.perform(post("/api/room-calendar/blocks").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "roomId": 1,
                          "blockType": "maintenance",
                          "startDate": "2026-10-01",
                          "endDateExclusive": "2026-10-02",
                          "reason": "点検"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.endDateExclusive").value("2026-10-02"));

        verify(service).createBlock(any(BlockWrite.class), eq("admin"));
    }

    @Test
    void inventoryConflictIsReturnedAs409() throws Exception {
        when(service.createBlock(any(BlockWrite.class), eq("admin")))
                .thenThrow(new InventoryConflictException("指定期間は予約済みです。"));

        mockMvc.perform(post("/api/room-calendar/blocks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "blockType": "stop_sale",
                                  "startDate": "2026-10-01",
                                  "endDateExclusive": "2026-10-02",
                                  "reason": "停售"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("指定期間は予約済みです。"));
    }
}
