package com.andrelucs.realtimepolls.data.dto;

public record PollOptionDTO(
        Long id,
        Long pollId,
        String description,
        int votes
) {
}
