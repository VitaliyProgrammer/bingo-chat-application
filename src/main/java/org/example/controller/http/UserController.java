package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserProfileResponseDto;
import org.example.dto.response.UserSearchResponseDto;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @Operation(summary = "Search users by nickname",
            description = "Search users using nickname with partial matching")
    public List<UserSearchResponseDto> searchProfile(String nickName) {

        return userService.searchByNickname(nickName);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Upload/Update user avatar",
            description = "Upload a new avatar image for the currently authenticated user.")
    public String updateAvatar(@RequestParam("file") MultipartFile file) {
        return userService.updateAvatar(file);
    }

    @DeleteMapping("/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user avatar",
            description = "Remove the avatar image of the currently authenticated user.")
    public void deleteAvatar() {
        userService.deleteAvatar();
    }
}
