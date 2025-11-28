package com.andrelucs.realtimepolls.integrationtests.pollstatussystem;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.integrationtests.AbstractIntegrationTest;
import com.andrelucs.realtimepolls.polls.PollRepository;
import com.andrelucs.realtimepolls.polls.scheduler.SmartStatusScheduler;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
public class SmartStatusSchedulerTest extends AbstractIntegrationTest {

    private final SmartStatusScheduler smartStatusScheduler;

    @Autowired
    public SmartStatusSchedulerTest(PollRepository pollRepository, SmartStatusScheduler smartStatusScheduler) {
        super(pollRepository);
        this.smartStatusScheduler = smartStatusScheduler;
    }

    @Override
    protected void saveTestData() {
//        super.saveTestData();
    }

    @BeforeEach
    void setupScheduler (){
        smartStatusScheduler.ensureIsListening();
        smartStatusScheduler.resetStatusToUpdate();
    }

    @Test
    void shouldTriggerProcessOnPastEventsOnInit() {
        // Evento no passado (deve ser processado automaticamente)
        var eventTime = LocalDateTime.now().minusHours(2);

        var poll = Poll.builder()
                .question("Test Past Event")
                .startDate(eventTime)
                .endDate(LocalDateTime.now().plusDays(1))
                .options(new ArrayList<>())
                .build();

        poll.getOptions().add(new PollOption(null, "A", 0, poll));
        poll.getOptions().add(new PollOption(null, "A", 0, poll));
        poll.getOptions().add(new PollOption(null, "A", 0, poll));
        pollRepository.save(poll);

        // Forçar a lógica de startup novamente (útil para testes)
        smartStatusScheduler.resetStatusToUpdate();
        smartStatusScheduler.processMostRecentStatus();

        // Esperar até que o evento antigo tenha sido processado
        Awaitility.await()
                .pollInterval(Duration.ofMillis(100))
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var processed = smartStatusScheduler.getStatusBeingProcessed();
                    assertNotNull(processed, "Old event should have been processed on startup");
                    Assertions.assertTrue(processed.getScheduledDate().isAfter(LocalDateTime.now()));
                });
    }


    @Test
    void shouldStartProcessignTheClosestEventOnInit() {
        var now = LocalDateTime.now();
        var mostRecentEventTime = now.plusHours(1);

        var poll = Poll.builder()
                .question("Event ordering test")
                .startDate(mostRecentEventTime)
                .endDate(now.plusDays(2))
                .options(new ArrayList<>())
                .build();

        poll.getOptions().add(new PollOption(null, "A", 0, poll));
        poll.getOptions().add(new PollOption(null, "A", 0, poll));
        poll.getOptions().add(new PollOption(null, "A", 0, poll));

        pollRepository.save(poll);

        smartStatusScheduler.resetStatusToUpdate();
        smartStatusScheduler.processMostRecentStatus();

        Awaitility.await()
                .pollInterval(Duration.ofMillis(100))
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var status = smartStatusScheduler.getStatusBeingProcessed();
                    assertNotNull(status);
                    Assertions.assertEquals(mostRecentEventTime.withNano(0), status.getScheduledDate().withNano(0));
                });
    }


    @Test
    void shouldProcessTheMostRecentEventComingFromAPostgresNotification() {
        // Will receive 2 event notifications and process only the closest one
        var mostRecentEventTime = LocalDateTime.now().plusHours(5);
        var poll = Poll.builder()
                .question("Qual banco de dados você mais utiliza?")
                .startDate(mostRecentEventTime)
                .endDate(LocalDateTime.now().plusDays(2))
                .options(new ArrayList<>())
                .build();

        var options = poll.getOptions();
        options.add(new PollOption(null, "PostgreSQL", 3, poll));
        options.add(new PollOption(null, "MySQL", 4, poll));
        options.add(new PollOption(null, "MongoDB", 1, poll));

        pollRepository.save(poll);

        var noNanoEventTime = mostRecentEventTime.withNano(0);

        log.info("Scheduled event date should be {}", noNanoEventTime);
        Awaitility.await()
            .pollInterval(Duration.ofMillis(100))
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                var status = smartStatusScheduler.getStatusBeingProcessed();
                assertNotNull(status, "Status should not be null after NOTIFY");

                LocalDateTime scheduled = status.getScheduledDate().withNano(0);

                Assertions.assertEquals(noNanoEventTime, scheduled, "Scheduler should process the most recent scheduled date");
            });
    }
}
