package com.andrelucs.realtimepolls.websocket.events;

import org.springframework.context.ApplicationEvent;

public abstract class WebSocketEvent <T> extends ApplicationEvent{

    private final T data;

    public WebSocketEvent(Object source, T data) {
        super(source);
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
