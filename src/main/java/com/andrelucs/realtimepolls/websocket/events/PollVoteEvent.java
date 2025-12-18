package com.andrelucs.realtimepolls.websocket.events;

import com.andrelucs.realtimepolls.websocket.data.PollOptionVoteDTO;

public class PollVoteEvent extends WebSocketEvent<PollOptionVoteDTO> {
    public PollVoteEvent(Object source, PollOptionVoteDTO data) {
        super(source, data);
    }
}
