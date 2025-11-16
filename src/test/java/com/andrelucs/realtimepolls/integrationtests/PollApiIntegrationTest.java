package com.andrelucs.realtimepolls.integrationtests;

import com.andrelucs.realtimepolls.data.dto.PollDTO;
import com.andrelucs.realtimepolls.data.dto.PollEditRequestDTO;
import com.andrelucs.realtimepolls.data.dto.PollRequestDTO;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.polls.PollRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
public class PollApiIntegrationTest extends AbstractIntegrationTest{

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EntityManager entityManager;

    @Autowired
    public PollApiIntegrationTest(PollRepository pollRepository) {
        super(pollRepository);
    }

    @Test
    void shouldCreatePoll() throws Exception {

        PollRequestDTO pollRequestDTO = new PollRequestDTO(
                "Qual a linguagem você prefere para backend?",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                List.of("Java", "Go", "Elixir"));
        String jsonRequest = objectMapper.writeValueAsString(pollRequestDTO);

        MvcResult result = mockMvc.perform(post("/api/poll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.options.length()").value(3))
                .andReturn();
        logResult(result);
    }

    @Test
    @Transactional
    void shouldEditAPoll() throws Exception {
        var pollToEdit = pollRepository.findAllByStatus(PollStatus.NOT_STARTED).getFirst();
        Hibernate.initialize(pollToEdit.getOptions());
        log.info("Poll to edit: {}", pollToEdit);
        entityManager.detach(pollToEdit);
        pollToEdit.getOptions().forEach(entityManager::detach);

        // Should update only the specified fields
        var updatedPollRequestDTO = new PollEditRequestDTO(
                "Qual lingugagem de programação você mais odeia?"+LocalDateTime.now(), // Using LocalDateTime.now() to ensure a difference with the current poll question
                null,
                LocalDateTime.now().plusMonths(1)
        );
        String jsonRequest = objectMapper.writeValueAsString(updatedPollRequestDTO);

        var result = mockMvc.perform(
                put("/api/poll/%d".formatted(pollToEdit.getId()))
                        .contentType(MediaType.APPLICATION_JSON).content(jsonRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pollToEdit.getId()))
                .andReturn();

        logResult(result);
        var edditedPoll = objectMapper.readValue(result.getResponse().getContentAsString(), PollDTO.class);
        log.info("Poll to edit: {}", pollToEdit);

        Assertions.assertNotEquals(pollToEdit.getQuestion(),edditedPoll.getQuestion());
        Assertions.assertNotEquals(pollToEdit.getEndDate(), edditedPoll.getEndDate());
        Assertions.assertNotNull(edditedPoll.getStartDate()); // Should not change
        Assertions.assertEquals(edditedPoll.getOptions().size(), pollToEdit.getOptions().size()); // Should not change
    }

    @Test
    void shouldFailToEditAStartedPoll() throws Exception {
        var pollToEdit = pollRepository.findAllByStatus(PollStatus.STARTED).getFirst();

        // Should update only the specified fields
        var updatedPollRequestDTO = new PollEditRequestDTO(
                "Qual lingugagem de programação você mais odeia?"+LocalDateTime.now(),
                null,
                LocalDateTime.now().plusMonths(1)
        );
        String jsonRequest = objectMapper.writeValueAsString(updatedPollRequestDTO);

        mockMvc.perform(put("/api/poll/%d".formatted(pollToEdit.getId())).contentType(MediaType.APPLICATION_JSON).content(jsonRequest))
            .andExpect(status().isForbidden());
    }


    @Test
    void shouldDeleteAPoll() throws Exception {
        var finishedPoll = pollRepository.findAllByStatus(PollStatus.FINISHED).getFirst();

        mockMvc.perform(delete("/api/poll/%d".formatted(finishedPoll.getId())))
                .andExpect(status().isNoContent());

        boolean exists = pollRepository.existsById(finishedPoll.getId());

        Assertions.assertFalse(exists);
    }

    @Test
    void shouldFindAllPolls() throws Exception {
        long amount = pollRepository.count();
        var result = mockMvc.perform(get("/api/poll"))
                .andExpect(jsonPath("$.length()").value(amount))
                .andReturn();

        logResult(result);
    }

    @Test
    void shouldFindPollsByStatus() throws Exception {
        int ongoingPollsCount = 2; // From the parent class beforeEach

        var result = mockMvc.perform(get("/api/poll").param("status", PollStatus.STARTED.toString()))
                .andExpect(jsonPath("$.length()").value(ongoingPollsCount))
                .andReturn();

        logResult(result);
    }

    private void logResult(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        log.info("REQUEST: {}", result.getRequest().getRequestURI());
        log.info("BODY: {}", result.getRequest().getContentAsString());
        log.info("PARAMS: {}", objectMapper.writeValueAsString(result.getRequest().getParameterMap()));
        log.info("CONTENT: {}", result.getResponse().getContentAsString());
        log.info("STATUS: {}", result.getResponse().getStatus());
    }
}

