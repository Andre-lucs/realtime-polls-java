package com.andrelucs.realtimepolls.unittests;

import com.andrelucs.realtimepolls.polloptions.OptionController;
import com.andrelucs.realtimepolls.polloptions.OptionService;
import com.andrelucs.realtimepolls.polloptions.PollOptionRepository;
import com.andrelucs.realtimepolls.polls.PollService;
import com.andrelucs.realtimepolls.data.dto.PollDTO;
import com.andrelucs.realtimepolls.data.dto.PollOptionDTO;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollUpdateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest(OptionController.class)
public class OptionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    PollService pollService;
    @MockitoBean
    OptionService optionService;
    @MockitoBean
    PollOptionRepository optionRepository;

    @BeforeEach
    void setUp() {
        reset(pollService, optionService);
    }

    @Test
    void shouldBeAbleToAddOptionsToANotStartedPoll() throws Exception {
        // escolhe um poll NOT_STARTED
        Long pollId = 1L;

        when(pollService.pollExists(eq(pollId))).thenReturn(true);
        when(optionService.addPollOption(eq(pollId), any())).thenReturn(List.of(
                new PollOptionDTO(1L, "Java", 0),
                new PollOptionDTO(2L, "Go", 0),
                new PollOptionDTO(3L, "Elixir", 0)
        ));

        var result = mockMvc.perform(post("/api/poll/%d/options".formatted(pollId))
                        .param("description", "C#"))
                .andExpect(status().isOk())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFailToAddOptionsToPollsIfPollDoesNotExist() throws Exception {
        Long pollId = 99L;
        when(pollService.pollExists(eq(pollId))).thenReturn(false);

        var result = mockMvc.perform(post("/api/poll/%d/options".formatted(pollId))
                        .param("description", "C#"))
                .andExpect(status().isNotFound())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFailToAddOptionsToPollsIfTheOperationIsInvalid() throws Exception {

        when(pollService.pollExists(any())).thenReturn(true);
        when(optionService.addPollOption(any(), any())).thenThrow(new InvalidPollUpdateException("Broke AddPoll rule"));

        var result = mockMvc.perform(post("/api/poll/%d/options".formatted(1L))
                        .param("description", "C#"))
                .andExpect(status().isForbidden())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldBeAbleToRemoveAOptionOfANON_STARTEDPoll() throws Exception {

        PollDTO validPoll = new PollDTO(6L, "What is your age range?",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                PollStatus.NOT_STARTED, List.of(
                new PollOptionDTO(1L, "0-10", 0),
                new PollOptionDTO(2L, "11-20", 0),
                new PollOptionDTO(3L, "30-50", 0),
                new PollOptionDTO(4L, "40+", 0)));

        when(pollService.pollExists(any())).thenReturn(true);

        // Should receive the remaining pollOptions
        var result = mockMvc.perform(delete("/api/poll/%d/options/%d".formatted(validPoll.getId(), validPoll.getOptions().getLast().getId())))
                .andExpect(status().isNoContent())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFailAtOptionRemovalIfPollDoesNotExist() throws Exception {
        when(pollService.pollExists(any())).thenReturn(false);
        var result = mockMvc.perform(delete("/api/poll/%d/options/%d".formatted(999L, 0L)))
                .andExpect(status().isNotFound())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFailAtOptionRemovalIfTheDeletionIsInvalid() throws Exception {
        when(pollService.pollExists(any())).thenReturn(true);
        doThrow(InvalidPollUpdateException.class).when(optionService).removePollOption(any());

        var result = mockMvc.perform(delete("/api/poll/%d/options/%d".formatted(1L, 1L)))
                .andExpect(status().isForbidden())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldBeAbleToVoteForAOptionFromAOngoingPoll() throws Exception{
        PollDTO validPoll = new PollDTO(6L, "What is your age range?",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                PollStatus.NOT_STARTED, List.of(
                new PollOptionDTO(1L, "0-10", 0),
                new PollOptionDTO(2L, "11-20", 0),
                new PollOptionDTO(3L, "30-50", 0),
                new PollOptionDTO(4L, "40+", 0)));

        PollOptionDTO optionToVote = validPoll.getOptions().get(1);

        when(pollService.pollExists(any())).thenReturn(true);
        when(optionService.voteForOption(optionToVote.getId())).thenReturn(optionToVote);

        // Should receive the remaining pollOptions
        var result = mockMvc.perform(patch("/api/poll/%d/options/%d".formatted(validPoll.getId(), validPoll.getOptions().getLast().getId())))
                .andExpect(status().isOk())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFailToVoteInAInvalidOption() throws Exception {

        when(pollService.pollExists(any())).thenReturn(false);

        mockMvc.perform(patch("/api/poll/%d/options/%d".formatted(99L, 222L)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/poll/%d/options/%d".formatted(99L, null)))
                .andExpect(status().isBadRequest());

    }

    private void logResult(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        log.info("REQUEST: {}", result.getRequest().getRequestURI());
        log.info("BODY: {}", result.getRequest().getContentAsString());
        log.info("PARAMS: {}", objectMapper.writeValueAsString(result.getRequest().getParameterMap()));
        log.info("CONTENT: {}", result.getResponse().getContentAsString());
        log.info("STATUS: {}", result.getResponse().getStatus());

    }

}
