package com.andrelucs.realtimepolls.data.dto;

import com.andrelucs.realtimepolls.data.model.PollStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PollDTO (
        Long id,
        String question,
        LocalDateTime startDate,
        LocalDateTime endDate,
        PollStatus status,
        List<PollOptionDTO> options
){

}
