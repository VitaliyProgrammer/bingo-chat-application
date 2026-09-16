package org.example.controller.http;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.example.dto.response.MessagePageResponseDto;
import org.example.security.AuthRateLimitFilter;
import org.example.security.jwt.JwtAuthenticationFilter;
import org.example.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = MessageController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, AuthRateLimitFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @Test
    void getChatMessages_defaultPaging_returnsOk() throws Exception {
        when(messageService.getChatMessages(anyLong(), anyInt(), anyInt()))
                .thenReturn(new MessagePageResponseDto(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/messages/chat/17"))
                .andExpect(status().isOk());
    }

    @Test
    void getChatMessages_sizeAboveMax_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/messages/chat/17").param("size", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getChatMessages_negativePage_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/messages/chat/17").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getChatMessages_zeroSize_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/messages/chat/17").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
