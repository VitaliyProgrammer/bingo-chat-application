package org.example.mapper;

import org.example.dto.response.FeedbackPageResponseDto;
import org.example.dto.response.FeedbackResponseDto;
import org.example.entity.Feedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "email", source = "emailSnapshot")
    FeedbackResponseDto toDto(Feedback feedback);

    default FeedbackPageResponseDto toPageDto(Page<Feedback> page) {

        return new FeedbackPageResponseDto(
                page.getContent().stream()
                        .map(this::toDto)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
