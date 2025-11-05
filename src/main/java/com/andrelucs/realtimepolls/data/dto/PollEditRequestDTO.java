package com.andrelucs.realtimepolls.data.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PollEditRequestDTO(
        @NotBlank(message = "Poll question should not be blank")
        String question,
        @NotNull(message = "Should specify a startDate for the poll")
        LocalDateTime startDate,
        @NotNull(message = "Should specify a endDate for the poll")
        LocalDateTime endDate)
{ }
