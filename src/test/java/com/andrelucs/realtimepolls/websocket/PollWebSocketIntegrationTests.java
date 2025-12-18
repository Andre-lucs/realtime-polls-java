package com.andrelucs.realtimepolls.websocket;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.integrationtests.AbstractIntegrationTest;
import com.andrelucs.realtimepolls.polloptions.OptionService;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class PollWebSocketIntegrationTests extends AbstractIntegrationTest {
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private final OptionService optionService;
    @LocalServerPort
    private int port;

    @Autowired
    public PollWebSocketIntegrationTests(PollRepository pollRepository, OptionService optionService) {
        super(pollRepository);
        this.optionService = optionService;
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
    void shouldReceivePollVoteUpdateOverWebSocket() throws Exception {
        // Arrange: create poll
        var scheduled = LocalDateTime.now().minusMinutes(3);
        var poll = Poll.builder()
                .question("Test poll")
                .startDate(scheduled)
                .endDate(scheduled.plusHours(1))
                .options(new ArrayList<>())
                .build();

        poll.getOptions().addAll(List.of(
                new PollOption(null, "A", 0, poll),
                new PollOption(null, "B", 0, poll),
                new PollOption(null, "C", 0, poll)
        ));
        poll = pollRepository.saveAndFlush(poll);

        String topic = "/topic/poll." + poll.getId() + ".votes";

        BlockingQueue<String> messages = new ArrayBlockingQueue<>(1);

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add(payload.toString());
            }
        });

        // Act: vote for a option
        optionService.voteForOption(poll.getOptions().getFirst().getId());

        // Assert — wait for WS notification
        String msg = messages.poll(6, TimeUnit.SECONDS);
        assertNotNull(msg, "WebSocket should receive a status update");
        log.info("The received socket message was: {}", msg);
    }

    @Test
    void shouldSendWebSocketMessagesForEachVoteInCorrectOrder() throws Exception {
        // Arrange
        var scheduled = LocalDateTime.now().minusMinutes(3);
        var poll = Poll.builder()
                .question("Ordered votes poll")
                .startDate(scheduled)
                .endDate(scheduled.plusHours(1))
                .options(new ArrayList<>())
                .build();

        poll.getOptions().addAll(List.of(
                new PollOption(null, "A", 0, poll),
                new PollOption(null, "B", 0, poll),
                new PollOption(null, "C", 0, poll)
        ));

        poll = pollRepository.saveAndFlush(poll);

        var optionA = poll.getOptions().getFirst();

        String topic = "/topic/poll." + poll.getId() + ".votes";

        BlockingQueue<Map<String, Object>> messages = new ArrayBlockingQueue<>(5);

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

        // Act — multiple votes
        optionService.voteForOption(optionA.getId()); // vote 1
        optionService.voteForOption(optionA.getId()); // vote 2
        optionService.voteForOption(optionA.getId()); // vote 3

        // Assert
        Map<String, Object> msg1 = messages.poll(5, TimeUnit.SECONDS);
        Map<String, Object> msg2 = messages.poll(5, TimeUnit.SECONDS);
        Map<String, Object> msg3 = messages.poll(5, TimeUnit.SECONDS);

        assertNotNull(msg1, "First WS message missing");
        assertNotNull(msg2, "Second WS message missing");
        assertNotNull(msg3, "Third WS message missing");

        // Aqui depende do formato do payload — ajuste conforme o seu DTO real
        Integer votes1 = (Integer) msg1.getOrDefault("votes", optionA.getVotes());
        Integer votes2 = (Integer) msg2.getOrDefault("votes", optionA.getVotes());
        Integer votes3 = (Integer) msg3.getOrDefault("votes", optionA.getVotes());

        assertEquals(1, votes1);
        assertEquals(2, votes2);
        assertEquals(3, votes3);

        log.info("Received WS messages in order: {}, {}, {}", votes1, votes2, votes3);
    }
    @Test
    void shouldSendWebSocketMessagesForConcurrentVotes() throws Exception {
        // Arrange
        var scheduled = LocalDateTime.now().minusMinutes(3);
        var poll = Poll.builder()
                .question("Concurrent votes poll")
                .startDate(scheduled)
                .endDate(scheduled.plusHours(1))
                .options(new ArrayList<>())
                .build();

        poll.getOptions().addAll(List.of(
                new PollOption(null, "A", 0, poll),
                new PollOption(null, "B", 0, poll),
                new PollOption(null, "C", 0, poll)
        ));

        poll = pollRepository.saveAndFlush(poll);

        var optionA = poll.getOptions().getFirst();

        String topic = "/topic/poll." + poll.getId() + ".votes";

        int concurrentVotes = 10;

        BlockingQueue<Map<String, Object>> messages = new ArrayBlockingQueue<>(concurrentVotes);

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

        // Act — concurrent voting
        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < concurrentVotes; i++) {
            executor.submit(() -> optionService.voteForOption(optionA.getId()));
        }

        executor.shutdown();
        assertTrue(
                executor.awaitTermination(5, TimeUnit.SECONDS),
                "Voting threads did not finish in time"
        );

        // Assert — collect messages
        List<Integer> receivedVotes = new ArrayList<>();

        for (int i = 0; i < concurrentVotes; i++) {
            Map<String, Object> msg = messages.poll(5, TimeUnit.SECONDS);
            assertNotNull(msg, "Missing WS message for vote " + (i + 1));

            Integer votes = (Integer) msg.get("votes");
            receivedVotes.add(votes);
            log.info("WebSocket message received : {}", msg);
        }

        // Validate results
        assertEquals(concurrentVotes, receivedVotes.size());

        int maxVotes = receivedVotes.stream().mapToInt(Integer::intValue).max().orElse(0);
        int minVotes = receivedVotes.stream().mapToInt(Integer::intValue).min().orElse(0);

        assertEquals(concurrentVotes, maxVotes,
                "Final vote count should match concurrent votes");

        assertEquals(1, minVotes,
                "Votes should start from 1");

        log.info("Received concurrent WS vote updates: {}", receivedVotes);
    }

}
