package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Operations related to users: profile management")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get current user profile",
            description = "Retrieve profile of the currently authenticated user.")
    public UserProfileResponseDto getUserProfile() {

        return userService.getCurrentUserProfile();
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Search users by nickName",
            description = "Search users using nickname with partial matching")
    public List<UserSearchResponseDto> searchProfile(String nickName) {

        return userService.searchByNickName(nickName);
    }
}
