package org.example.service;

import org.example.dto.request.UserLoginRequestDto;
import org.example.dto.request.UserRegistrationRequestDto;
import org.example.dto.response.UserLoginResponseDto;
import org.example.dto.response.UserRegistrationResponseDto;

public interface AuthenticationService {
    UserRegistrationResponseDto registration(UserRegistrationRequestDto request);

    UserLoginResponseDto login(UserLoginRequestDto request);
}
