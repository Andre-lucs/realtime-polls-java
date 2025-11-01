package com.andrelucs.realtimepolls.data.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PollRequestDTO(
        String question,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<String> options)
{ }
