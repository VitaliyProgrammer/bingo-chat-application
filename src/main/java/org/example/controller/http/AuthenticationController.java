package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
