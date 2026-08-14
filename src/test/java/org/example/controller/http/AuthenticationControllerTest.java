package org.example.controller.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.dto.response.UserLoginResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;
import org.example.exception.AuthenticationException;
import org.example.exception.RegistrationException;
import org.example.security.AuthRateLimitFilter;
import org.example.security.jwt.JwtAuthenticationFilter;
import org.example.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AuthenticationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, AuthRateLimitFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void registration_validRequest_returnsCreated() throws Exception {
        when(authenticationService.registration(any()))
                .thenReturn(new UserRegistrationResponseDto(1L, "user@example.com",
                        "John", "Doe", "johnny"));

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("user@example.com", "password123",
                                "password123")))
                .andExpect(status().isCreated());
    }

    @Test
    void registration_duplicateEmail_returnsBadRequestWithRegistrationFailedCode()
            throws Exception {
        when(authenticationService.registration(any()))
                .thenThrow(new RegistrationException("Email already exists!"));

        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("user@example.com", "password123",
                                "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REGISTRATION_FAILED"));
    }

    @Test
    void registration_emailWithLeadingWhitespace_isRejectedByBeanValidation() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(" user@example.com", "password123",
                                "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void registration_passwordsDoNotMatch_isRejectedByBeanValidation() throws Exception {
        mockMvc.perform(post("/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("user@example.com", "password123",
                                "different456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_validCredentials_returnsOkWithToken() throws Exception {
        when(authenticationService.login(any()))
                .thenReturn(new UserLoginResponseDto("jwt-token-value"));

        mockMvc.perform(post("/auth/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@example.com", "password": "password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        when(authenticationService.login(any()))
                .thenThrow(new AuthenticationException("Invalid email or password!"));

        mockMvc.perform(post("/auth/authentication")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@example.com", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    private String registrationJson(String email, String password, String repeatPassword) {
        return """
                {
                    "email": "%s",
                    "password": "%s",
                    "repeatPassword": "%s",
                    "firstName": "John",
                    "lastName": "Doe",
                    "nickName": "johnny"
                }
                """.formatted(email, password, repeatPassword);
    }
}
