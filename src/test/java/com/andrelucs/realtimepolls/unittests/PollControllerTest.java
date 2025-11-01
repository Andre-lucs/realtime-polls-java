package com.andrelucs.realtimepolls.unittests;

import com.andrelucs.realtimepolls.exceptions.service.InvalidPollEditException;
import com.andrelucs.realtimepolls.polls.PollController;
import com.andrelucs.realtimepolls.polls.PollService;
import com.andrelucs.realtimepolls.data.dto.PollDTO;
import com.andrelucs.realtimepolls.data.dto.PollRequestDTO;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollOpinionsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@WebMvcTest(PollController.class)
public class PollControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    PollService pollService;

    List<PollDTO> polls;

    @BeforeEach
    void setUp() {
        reset(pollService);
        polls = List.of(
                new PollDTO(1L, "question text", LocalDateTime.of(2025, 10, 30, 12, 0), LocalDateTime.of(2025, 11, 1, 12, 0), PollStatus.NOT_STARTED, new ArrayList<>() ),
                new PollDTO(2L, "question text 2", LocalDateTime.of(2025, 10, 30, 12, 0), LocalDateTime.of(2025, 11, 1, 12, 0), PollStatus.NOT_STARTED, new ArrayList<>() ),
                new PollDTO(3L, "question text 3", LocalDateTime.of(2024, 10, 30, 12, 0), LocalDateTime.of(2024, 11, 1, 12, 0), PollStatus.FINISHED, new ArrayList<>() )
        );
    }

    // Find requests

    @Test
    void shouldFindAPostByItsId() throws Exception {
        when(pollService.findById(eq(2L))).thenReturn(Optional.of(polls.get(1)));

        String expectedPoll = objectMapper.writeValueAsString(polls.get(1));

        var result = mockMvc.perform(get("/api/poll/2"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedPoll))
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFailToFindAInexistentPoll() throws Exception {
        when(pollService.findById(eq(999L))).thenReturn(Optional.empty());

        var result = mockMvc.perform(get("/api/poll/999"))
                .andExpect(status().isNotFound())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFindAllPolls() throws Exception {
        when(pollService.findAll()).thenReturn(polls);

        String expectedPolls = objectMapper.writeValueAsString(polls);

        var result = mockMvc.perform(get("/api/poll"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedPolls))
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFindPollsByStatusIfSpecified() throws Exception {
        List<PollDTO> finished = polls.stream().filter(p -> p.status() == PollStatus.FINISHED).toList();
        when(pollService.findByStatus(eq(PollStatus.FINISHED))).thenReturn(finished);

        String expectedPolls = objectMapper.writeValueAsString(finished);

        var result = mockMvc.perform(get("/api/poll?status=FINISHED"))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedPolls))
                .andReturn();

        logResult(result);
    }

    // Creation requests

    @Test
    void shouldCreateAPoll() throws Exception {
        // Simula que o serviço criou o último poll da lista
        when(pollService.save(any())).thenReturn(polls.getLast());

        PollRequestDTO pollRequestDTO = new PollRequestDTO(
                "Qual a linguagem você prefere para backend?",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of("Java", "Go", "Elixir"));

        String jsonRequest = objectMapper.writeValueAsString(pollRequestDTO);

        var result = mockMvc.perform(post("/api/poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldNotCreateAInvalidPoll() throws Exception {
        // This example shows a invalid opinions count
        when(pollService.save(any())).thenThrow(new InvalidPollOpinionsException(3, 2));

        PollRequestDTO pollRequestDTO = new PollRequestDTO(
                "Qual a linguagem você prefere para backend?",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of("Java", "Go"));

        String jsonRequest = objectMapper.writeValueAsString(pollRequestDTO);
        var result = mockMvc.perform(post("/api/poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldDeleteAPoll() throws Exception {
        var result = mockMvc.perform(delete("/api/poll/%d".formatted(1L)))
                .andExpect(status().isNoContent())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldBeAbleToEditAPoll() throws Exception {
        when(pollService.editPoll(any(), any())).thenReturn(polls.getLast());

        PollRequestDTO pollRequestDTO = new PollRequestDTO(
                "Qual a linguagem você prefere para backend?",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of("Java", "Go", "Elixir"));

        String jsonRequest = objectMapper.writeValueAsString(pollRequestDTO);

        var result = mockMvc.perform(put("/api/poll/%d".formatted(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFailAtEditWhenPollIsStarted() throws Exception {
        when(pollService.editPoll(any(), any())).thenThrow(new InvalidPollEditException("Poll already started"));

        PollRequestDTO pollRequestDTO = new PollRequestDTO(
                "Qual a linguagem você prefere para backend?",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of("Java", "Go", "Elixir"));

        String jsonRequest = objectMapper.writeValueAsString(pollRequestDTO);

        var result = mockMvc.perform(put("/api/poll/%d".formatted(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden())
                .andReturn();

        logResult(result);
    }

    // Options edit


    private void logResult(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        log.info("REQUEST: {}", result.getRequest().getRequestURI());
        log.info("BODY: {}", result.getRequest().getContentAsString());
        log.info("PARAMS: {}", objectMapper.writeValueAsString(result.getRequest().getParameterMap()));
        log.info("CONTENT: {}", result.getResponse().getContentAsString());
        log.info("STATUS: {}", result.getResponse().getStatus());

    }
}
