package com.andrelucs.realtimepolls.integrationtests;

import com.andrelucs.realtimepolls.data.dto.PollRequestDTO;
import com.andrelucs.realtimepolls.polls.PollRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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

    private void logResult(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        log.info("REQUEST: {}", result.getRequest().getRequestURI());
        log.info("BODY: {}", result.getRequest().getContentAsString());
        log.info("PARAMS: {}", objectMapper.writeValueAsString(result.getRequest().getParameterMap()));
        log.info("CONTENT: {}", result.getResponse().getContentAsString());
        log.info("STATUS: {}", result.getResponse().getStatus());
    }
}

