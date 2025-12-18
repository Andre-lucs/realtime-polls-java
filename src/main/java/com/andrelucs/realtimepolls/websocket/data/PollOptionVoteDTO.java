package com.andrelucs.realtimepolls.websocket.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public final class PollOptionVoteDTO {
    private final Long pollId;
    private final Long optionId;
    private Long votes;
    private final Long delta;
    private final LocalDateTime timestamp;
}
