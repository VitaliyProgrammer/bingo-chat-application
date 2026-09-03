package org.example.controller.http;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.security.AuthRateLimitFilter;
import org.example.security.jwt.JwtAuthenticationFilter;
import org.example.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AdminChatController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, AuthRateLimitFilter.class}))
@Import(AdminChatControllerTest.MethodSecurityConfig.class)
class AdminChatControllerTest {

    private static final Long CHAT_ID = 1L;
    private static final String REQUEST_BODY = "{\"reason\": \"Spam reported by multiple users\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockGroupChat_withAdminRole_returnsNoContent() throws Exception {
        mockMvc.perform(post("/admin/chats/{chatId}/block", CHAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isNoContent());

        verify(chatService).blockGroupChat(anyLong(), anyString());
    }

    @Test
    @WithMockUser(roles = "USER")
    void blockGroupChat_withoutAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/chats/{chatId}/block", CHAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isForbidden());

        verify(chatService, never()).blockGroupChat(anyLong(), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unblockGroupChat_withAdminRole_returnsNoContent() throws Exception {
        mockMvc.perform(post("/admin/chats/{chatId}/unblock", CHAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isNoContent());

        verify(chatService).unblockGroupChat(anyLong(), anyString());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unblockGroupChat_withoutAdminRole_returnsForbidden() throws Exception {
        mockMvc.perform(post("/admin/chats/{chatId}/unblock", CHAT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isForbidden());

        verify(chatService, never()).unblockGroupChat(anyLong(), anyString());
    }
}
