package com.andrelucs.realtimepolls.integrationtests;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.polloptions.PollOptionRepository;
import com.andrelucs.realtimepolls.polls.PollRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
public class PollOptionsIntegrationTest extends AbstractIntegrationTest{

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EntityManager entityManager;
    @Autowired
    PollOptionRepository optionRepository;

    private Poll poll;

    @Autowired
    public PollOptionsIntegrationTest(PollRepository pollRepository) {
        super(pollRepository);
    }

    @BeforeEach
    @Override
    protected void saveTestData() {
        // Create a fresh poll with 5 options

        var poll = Poll.builder()
                .question("Integration Test Poll")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .options(new ArrayList<>())
                .build();

        IntStream.range(0, 5).forEach(i -> {
            var option = new PollOption();
            option.setDescription("Option " + i);
            option.setPoll(poll);
            poll.getOptions().add(option);
        });

        this.poll = pollRepository.saveAndFlush(poll);
    }

    @Test
    void shouldBeAbleToAddVariousOptions() throws Exception {
        var pollToEdit = this.poll;

        log.info("Options before adding: {}", pollToEdit.getOptions());

        List<String> optionsToAdd = IntStream.range(0, 10)
                .mapToObj("new Option number : %d"::formatted)
                .toList();

        int initialOptionsCount = pollToEdit.getOptions().size();
        int expectedOptionsCount = initialOptionsCount + optionsToAdd.size();

        for (String option : optionsToAdd) {
            mockMvc.perform(post("/api/poll/%d/options".formatted(pollToEdit.getId()))
                            .param("description", option))
                    .andExpect(status().isOk());
        }

        // Force reload from DB
        var currentOptions = optionRepository.findAllByPollId(pollToEdit.getId());

        log.info("After adding options: {}", currentOptions);
        Assertions.assertEquals(expectedOptionsCount, currentOptions.size());

    }

    @Test
    void shouldBeAbleToDeleteOptions() throws Exception {
        var pollToEdit = this.poll;

        log.info("Target Poll: {}", pollToEdit);
        log.info("Options before deleting: {}", pollToEdit.getOptions());

        var optionToDelete = pollToEdit.getOptions().getFirst();

        log.info("All polls before delete option : {} ", pollRepository.findAll());
        log.info("Options before delete: {}", optionRepository.findAll());
        mockMvc.perform(delete("/api/poll/%d/options/%d".formatted(pollToEdit.getId(), optionToDelete.getId())))
                .andExpect(status().isNoContent());

        boolean exists = optionRepository.existsById(optionToDelete.getId());
        log.info("All polls after delete option : {} ", pollRepository.findAll());
        log.info("Options after delete: {}", optionRepository.findAll());
        Assertions.assertFalse(exists);
        Assertions.assertTrue(pollRepository.existsById(pollToEdit.getId())); // Should not delete
    }

    @Test
    void shouldNotDeleteOptionIfTheRemainingOptionsBeLessThan3() throws Exception {
        var pollToEdit = this.poll;

        // Delete until only 3 remain
        int amountToDelete = pollToEdit.getOptions().size() - 3;
        for (int i = 0; i < amountToDelete; i++) {
            var optionToDelete = pollToEdit.getOptions().removeFirst();
            mockMvc.perform(delete("/api/poll/%d/options/%d".formatted(pollToEdit.getId(), optionToDelete.getId())))
                    .andExpect(status().isNoContent());
        }
        Assertions.assertEquals(3, pollToEdit.getOptions().size());

        var optionToDelete = pollToEdit.getOptions().getFirst();

        // Attempt to delete when only 3 remain
        mockMvc.perform(delete("/api/poll/%d/options/%d".formatted(pollToEdit.getId(), optionToDelete.getId())))
                .andExpect(status().isForbidden());

        // Ensures the poll still has 3 options
        var remainingOptions = optionRepository.findAllByPollId(pollToEdit.getId());
        Assertions.assertEquals(3, remainingOptions.size());
    }


    @Test
    void shouldBeAbleToVoteForAOptionOnAOngoingPoll() throws Exception {
        super.saveTestData();
        var poll = pollRepository.findAllByStatus(PollStatus.STARTED).getFirst();
        var optionToVote = optionRepository.findAllByPollId(poll.getId()).getFirst();

        mockMvc.perform(patch("/api/poll/%d/options/%d".formatted(poll.getId(), optionToVote.getId())));

        PollOption repoPoll = optionRepository.findById(optionToVote.getId()).orElseThrow();

        Assertions.assertEquals(optionToVote.getVotes()+1, repoPoll.getVotes());
    }

    @Test
    void shouldFailAtVotingInAFinishedOrStartedPoll() throws Exception {
        var nonStartedPoll = pollRepository.findAllByStatus(PollStatus.NOT_STARTED).getFirst();

        var optionToVote = optionRepository.findAllByPollId(nonStartedPoll.getId()).getFirst();

        mockMvc.perform(patch("/api/poll/%d/options/%d".formatted(this.poll.getId(), optionToVote.getId())))
                .andExpect(status().isForbidden());

        PollOption repoPoll = optionRepository.findById(optionToVote.getId()).orElseThrow();

        Assertions.assertEquals(optionToVote.getVotes(), repoPoll.getVotes());

    }

}
