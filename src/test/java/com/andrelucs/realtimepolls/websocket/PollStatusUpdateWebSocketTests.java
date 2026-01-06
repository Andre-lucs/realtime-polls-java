package com.andrelucs.realtimepolls.websocket;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.integrationtests.AbstractIntegrationTest;
import com.andrelucs.realtimepolls.polls.PollRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class PollStatusUpdateWebSocketTests extends AbstractIntegrationTest {
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    @LocalServerPort
    private int port;

    @Autowired
    public PollStatusUpdateWebSocketTests(PollRepository pollRepository) {
        super(pollRepository);
    }

    @BeforeEach
    void connectWebSocket() throws Exception {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompSession = stompClient
                .connectAsync("ws://localhost:%d/ws/websocket".formatted(port), new WebSocketHttpHeaders(), new StompSessionHandlerAdapter() {})
                .get(2, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnect() {
        if (stompSession != null) stompSession.disconnect();
    }

    @Test
    void shouldSendWebSocketEventWhenPollStatusChanges() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Poll poll = Poll.builder()
                .question("Status WS poll")
                .startDate(now.plusSeconds(5))
                .endDate(now.plusSeconds(10))
                .options(new ArrayList<>())
                .build();

        poll.getOptions().add(new PollOption(null, "A", 0, poll));
        poll.getOptions().add(new PollOption(null, "B", 0, poll));
        poll.getOptions().add(new PollOption(null, "C", 0, poll));

        poll = pollRepository.saveAndFlush(poll);

        String topic = "/topic/poll." + poll.getId() + ".status";

        BlockingQueue<Map<String, Object>> messages = new ArrayBlockingQueue<>(2);

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Map<String, Object> message = (Map<String, Object>) payload;
                messages.add(message);
            }
        });
        log.info("TEST SUBSCRIBED TOPIC: {}", topic);

        // Assert — STARTED
        var openMsg = messages.poll(10, TimeUnit.SECONDS);
        assertNotNull("Expected STARTED status message", openMsg);

        assertEquals("STARTED", openMsg.get("toStatus"));

        // Assert — FINISHED
        var closeMsg = messages.poll(10, TimeUnit.SECONDS);
        assertNotNull("Expected FINISHED status message", closeMsg);

        assertEquals("FINISHED", closeMsg.get("toStatus"));

        log.info("Received poll status WS events: {}, {}", openMsg, closeMsg);
    }
    @Test
    void shouldNotSendStatusWebSocketEventIfPollAlreadyStarted() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Poll poll = Poll.builder()
                .question("Already started poll")
                .startDate(now.minusSeconds(10)) // already STARTED
                .endDate(now.plusSeconds(10))
                .options(new ArrayList<>())
                .build();

        poll.getOptions().add(new PollOption(null, "A", 0, poll));
        poll.getOptions().add(new PollOption(null, "B", 0, poll));
        poll.getOptions().add(new PollOption(null, "C", 0, poll));

        poll = pollRepository.saveAndFlush(poll);

        String topic = "/topic/poll." + poll.getId() + ".status";

        BlockingQueue<Map<String, Object>> messages = new ArrayBlockingQueue<>(1);

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((Map<String, Object>) payload);
            }
        });

        // Assert — should NOT receive any status message
        Map<String, Object> msg = messages.poll(5, TimeUnit.SECONDS);

        assertNull(
                msg,
                "Poll already started should not emit status WebSocket events"
        );

        log.info("No WS status event emitted for already started poll, as expected");
    }

}
