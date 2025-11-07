package com.andrelucs.realtimepolls.integrationtests;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.polls.PollRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
public class PollRepositoryStatusUpdateTests extends AbstractIntegrationTest{

    @PersistenceContext
    EntityManager em;

    @Autowired
    public PollRepositoryStatusUpdateTests(PollRepository pollRepository) {
        super(pollRepository);
    }


    @BeforeEach
    void saveRepoTestData() {

        // Poll que começa NOT_STARTED e vira STARTED
        Poll pollNotStarted = Poll.builder()
                .question("Poll que vira STARTED")
                .startDate(LocalDateTime.now().minusMinutes(1))
                .endDate(LocalDateTime.now().plusHours(5))
                .options(new ArrayList<>())
                .build();
        pollNotStarted.addOption(new PollOption(null, "Opção A", 0, pollNotStarted));
        pollNotStarted.addOption(new PollOption(null, "Opção B", 0, pollNotStarted));
        pollNotStarted.addOption(new PollOption(null, "Opção C", 0, pollNotStarted));

        // Poll que começa STARTED e vira FINISHED
        Poll pollStarted = Poll.builder()
                .question("Poll que vira FINISHED")
                .startDate(LocalDateTime.now().minusMinutes(100))
                .endDate(LocalDateTime.now().minusSeconds(10))
                .options(new ArrayList<>())
                .build();
        pollStarted.addOption(new PollOption(null, "Opção X", 0, pollStarted));
        pollStarted.addOption(new PollOption(null, "Opção Y", 0, pollStarted));
        pollStarted.addOption(new PollOption(null, "Opção Z", 0, pollStarted));

        // This updates the status to the correct ones
        var savedPoll = pollRepository.saveAll(List.of(pollNotStarted, pollStarted));

        // To test the update status put the wrong status manually

        em.createNativeQuery("UPDATE poll SET status = 'NOT_STARTED' WHERE id = :id")
                .setParameter("id", pollNotStarted.getId())
                .executeUpdate();

        em.createNativeQuery("UPDATE poll SET status = 'STARTED' WHERE id = :id")
                .setParameter("id", pollStarted.getId())
                .executeUpdate();

        em.clear(); // limpa cache do JPA

    }

    @Test
    @Transactional
    void shouldRecalculatePollFromNotStartedToStarted() {
        Poll poll = pollRepository.findAll().stream()
                .filter(p -> p.getQuestion().contains("STARTED"))
                .findFirst().orElseThrow();

        Assertions.assertEquals(PollStatus.NOT_STARTED, poll.getStatus());

        int updatedCount = pollRepository.recalculateStatusById(poll.getId());
        Assertions.assertEquals(1, updatedCount);

        em.clear();

        Poll updated = pollRepository.findById(poll.getId()).orElseThrow();

        var polls = pollRepository.findAll();
        log.info("Polls: {}", polls);

        Assertions.assertEquals(PollStatus.STARTED, updated.getStatus());
    }

    @Test
    @Transactional
    void shouldRecalculatePollFromStartedToFinished() {
        Poll poll = pollRepository.findAll().stream()
                .filter(p -> p.getQuestion().contains("FINISHED"))
                .findFirst().orElseThrow();

        Assertions.assertEquals(PollStatus.STARTED, poll.getStatus());

        int updatedCount = pollRepository.recalculateStatusById(poll.getId());
        Assertions.assertEquals(1, updatedCount);

        em.clear();

        Poll updated = pollRepository.findById(poll.getId()).orElseThrow();

        Assertions.assertEquals(PollStatus.FINISHED, updated.getStatus());
    }

    @Test
    @Transactional
    void shouldRecalculateAllStatuses() {
        var polls = pollRepository.findAll();
        var oldStatuses = polls.stream().map(Poll::getStatus).toList();

        int updatedCount = pollRepository.recalculateAllStatuses();
        Assertions.assertEquals(2, updatedCount); // Update the 2 setup for this test class
        em.clear();
        var updatedPolls = pollRepository.findAll();
        var newStatuses = updatedPolls.stream().map(Poll::getStatus).toList();
        Assertions.assertNotEquals(oldStatuses, newStatuses);
    }
}
