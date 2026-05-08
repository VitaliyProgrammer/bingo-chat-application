package org.example.mapper;

import org.example.dto.response.FeedbackResponseDto;
import org.example.entity.Feedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    FeedbackResponseDto toDto(Feedback feedback);
}
