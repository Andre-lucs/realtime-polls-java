package com.andrelucs.realtimepolls.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PollEditRequestDTO(
        String question,
        LocalDateTime startDate,
        LocalDateTime endDate)
{ }
