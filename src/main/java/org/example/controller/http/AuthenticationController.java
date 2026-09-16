package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.RefreshTokenRequestDto;
import org.example.dto.request.UserLoginRequestDto;
import org.example.dto.request.UserRegistrationRequestDto;
import org.example.dto.response.UserLoginResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;
import org.example.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "User", description = "Operations related to users: profile management")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registration a new user",
            description = "Registration a new user in the chat system")
    public UserRegistrationResponseDto registration(
            @RequestBody @Valid UserRegistrationRequestDto request) {

        return authenticationService.registration(request);
    }

    @PostMapping("/authentication")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authentication an existing user",
            description = "Authentication an existing user in the chat system")
    public UserLoginResponseDto login(
            @RequestBody @Valid UserLoginRequestDto request) {

        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new access/refresh token pair. "
                    + "The refresh token used in the call is invalidated immediately (rotation).")
    public UserLoginResponseDto refresh(
            @RequestBody @Valid RefreshTokenRequestDto request) {

        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out",
            description = "Invalidates the given refresh token, ending that session")
    public void logout(
            @RequestBody @Valid RefreshTokenRequestDto request) {

        authenticationService.logout(request);
    }
}
