package org.example.service;

import java.util.List;
import org.example.dto.request.FeedbackRequestDto;
import org.example.dto.request.FeedbackUpdateRequestDto;
import org.example.dto.response.AdminFeedbackStatsResponseDto;
import org.example.dto.response.FeedbackPageResponseDto;
import org.example.dto.response.FeedbackResponseDto;

public interface FeedbackService {

    FeedbackResponseDto createFeedback(FeedbackRequestDto request);

    List<FeedbackResponseDto> getMyFeedback();

    FeedbackPageResponseDto getAllFeedback(int page, int size);

    FeedbackResponseDto updateFeedback(Long feedbackId, FeedbackUpdateRequestDto request);

    AdminFeedbackStatsResponseDto getFeedbackStats();
}
