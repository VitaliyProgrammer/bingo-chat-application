package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.FeedbackRequestDto;
import org.example.dto.response.FeedbackResponseDto;
import org.example.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback API", description = "API for the bugs, questions and feature suggestions")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send feedback message",
            description = "Allows authenticated users to send message feedback message "
                    + "about the bugs, questions and feature suggestions")
    public FeedbackResponseDto createFeedBack(@RequestBody FeedbackRequestDto request) {

        return feedbackService.createFeedback(request);
    }
}
