package com.andrelucs.realtimepolls.websocket.events;

import com.andrelucs.realtimepolls.websocket.data.PollStatusUpdateDTO;

public class PollStatusEvent extends WebSocketEvent<PollStatusUpdateDTO>{
    public PollStatusEvent(Object source, PollStatusUpdateDTO data) {
        super(source, data);
    }
}
