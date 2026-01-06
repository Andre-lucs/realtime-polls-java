package com.andrelucs.realtimepolls.websocket.data;

import com.andrelucs.realtimepolls.data.model.PollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class PollStatusUpdateDTO {
    private Long pollId;
    private PollStatus fromStatus;
    private PollStatus toStatus;
    private LocalDateTime timestamp;
}
