package org.example.service;

import org.example.dto.request.FeedbackRequestDto;
import org.example.dto.response.FeedbackResponseDto;

public interface FeedbackService {

    FeedbackResponseDto createFeedback(FeedbackRequestDto request);
}
