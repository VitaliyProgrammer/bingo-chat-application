package org.example.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.FeedbackUpdateRequestDto;
import org.example.dto.response.AdminFeedbackStatsResponseDto;
import org.example.dto.response.FeedbackPageResponseDto;
import org.example.dto.response.FeedbackResponseDto;
import org.example.service.FeedbackService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/feedback")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Feedback API", description = "Support ticket management for administrators")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get feedback stats",
            description = "Returns counters for the admin dashboard, e.g. pending ticket count")
    public AdminFeedbackStatsResponseDto getFeedbackStats() {

        return feedbackService.getFeedbackStats();
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get all feedback",
            description = "Returns a paginated list of feedback submitted by all users")
    public FeedbackPageResponseDto getAllFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return feedbackService.getAllFeedback(page, size);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Respond to feedback",
            description = "Updates the status of a feedback ticket and/or attaches an admin reply")
    public FeedbackResponseDto updateFeedback(@PathVariable Long id,
                                              @Valid @RequestBody FeedbackUpdateRequestDto request) {

        return feedbackService.updateFeedback(id, request);
    }
}
