package com.andrelucs.realtimepolls.integrationtests;

import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.polls.PollRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class PollRepositoryTests extends AbstractIntegrationTest{

    @PersistenceContext
    EntityManager em;

    @Autowired
    public PollRepositoryTests(PollRepository pollRepository) {
        super(pollRepository);
    }

    @Test
    @Transactional
    void shouldFindAllPolls() {
        var result = pollRepository.findAll();
        Assertions.assertEquals(4, result.size());
        log.info("All polls: {}", result);
    }

    @Test
    @Transactional
    void shouldFindPollsByStatus() {
        var startedPolls = pollRepository.findAllByStatus(PollStatus.STARTED);
        Assertions.assertTrue(startedPolls.stream()
                .allMatch(p -> p.getStatus() == PollStatus.STARTED));

        log.info("Started polls: {}", startedPolls);
    }


}
