package com.andrelucs.realtimepolls.integrationtests.pollstatussystem;

import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.integrationtests.AbstractIntegrationTest;
import com.andrelucs.realtimepolls.polls.PollRepository;
import com.andrelucs.realtimepolls.polls.scheduler.StatusToUpdateRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
@Slf4j
public class PollStatusMigrationTest extends AbstractIntegrationTest {
    @Autowired
    private StatusToUpdateRepository statusToUpdateRepository;

    @Autowired
    public PollStatusMigrationTest(PollRepository pollRepository) {
        super(pollRepository);
    }

    @Test
    void shouldTriggerStatusToUpdateShceduleAddingToTheTable() {
        int nonStartedToStartedCount = pollRepository.findAllByStatus(PollStatus.NOT_STARTED).size();
        int startedToFinishedCount = nonStartedToStartedCount + pollRepository.findAllByStatus(PollStatus.STARTED).size();

        var statusChangeScheduledToStart = statusToUpdateRepository.findAllByNextStatus(PollStatus.STARTED);
        var statusChangeScheduledToFinish = statusToUpdateRepository.findAllByNextStatus(PollStatus.FINISHED);
        log.info("Start events : {}", statusChangeScheduledToStart);
        log.info("Finish events : {}", statusChangeScheduledToFinish);

        Assertions.assertEquals(nonStartedToStartedCount, statusChangeScheduledToStart.size());
        Assertions.assertEquals(startedToFinishedCount, statusChangeScheduledToFinish.size());
    }

    @Test
    void shouldTriggerStatusToUpdateDeletionOnPollDeletion() {
        var poll = pollRepository.findAllByStatus(PollStatus.NOT_STARTED).getFirst();

        var currentUpdates = statusToUpdateRepository.findAllByPoll(poll);

        Assertions.assertFalse(currentUpdates.isEmpty()); // Should not be empty for the test to be meaningfull

        pollRepository.deleteById(poll.getId());

        var remaining = statusToUpdateRepository.findAllByPoll(poll);

        Assertions.assertTrue(remaining.isEmpty());
    }

    @Test
    void shouldTriggerStatusToUpdateChangeOnPollDateRangeChanging() {
        var poll = pollRepository.findAllByStatus(PollStatus.STARTED).getFirst();

        var currentUpdates = statusToUpdateRepository.findAllByPoll(poll);
        // Should only have the event from Started to finished scheduled
        Assertions.assertEquals(1, currentUpdates.size());

        poll.setStartDate(LocalDateTime.now().plusHours(1));
        poll.setEndDate(LocalDateTime.now().plusHours(2));

        pollRepository.save(poll);

        var afterUpdate = statusToUpdateRepository.findAllByPoll(poll);
        // Should have the event from NotStarted to Started and Started to finished scheduled
        Assertions.assertEquals(2, afterUpdate.size());
    }
}
