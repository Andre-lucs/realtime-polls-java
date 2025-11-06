package com.andrelucs.realtimepolls.unittests;


import com.andrelucs.realtimepolls.config.ModelMapperConfiguration;
import com.andrelucs.realtimepolls.data.dto.PollOptionDTO;
import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollUpdateException;
import com.andrelucs.realtimepolls.polloptions.OptionService;
import com.andrelucs.realtimepolls.polloptions.PollOptionRepository;
import com.andrelucs.realtimepolls.polls.PollRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OptionService.class, ModelMapperConfiguration.class})
@Slf4j
class PollOptionServiceTest {

    @MockitoBean
    PollOptionRepository pollOptionRepository;

    @MockitoBean
    PollRepository pollRepository;

    @Autowired
    OptionService optionService;

    @BeforeEach
    void setUp() {
        reset();
    }

    @Test
    void shouldAddPollOptionAndReturnUpdatedList() {
        Long pollId = 1L;
        String description = "Nova opção";

        Poll poll = Poll.builder()
                .id(pollId)
                .status(PollStatus.NOT_STARTED)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .build();

        PollOption savedOption = PollOption.builder()
                .id(9L)
                .description(description)
                .poll(Poll.builder().id(pollId).build())
                .build();

        List<PollOption> currentOptions = new ArrayList<>();

        currentOptions.add(PollOption.builder().id(6L).description("Op 6").build());
        currentOptions.add(PollOption.builder().id(7L).description("Op 7").build());
        currentOptions.add(PollOption.builder().id(8L).description("Op 8").build());

        List<PollOption> optionsAfterSave = new ArrayList<>(currentOptions);
        optionsAfterSave.add(savedOption);

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll)); // Simulate finding the poll
        when(pollOptionRepository.existsByPollIdAndDescription(any(),any())).thenReturn(false); // Don't find a duplicate
        when(pollOptionRepository.saveAndFlush(any(PollOption.class))) // Save
                .thenReturn(savedOption);
        when(pollOptionRepository.findAllByPollId(pollId)) // Get all the options
                .thenReturn(optionsAfterSave);

        List<PollOptionDTO> result = optionService.addPollOption(pollId, description);

        log.info(optionsAfterSave.toString());
        log.info(result.toString());

        assertEquals(4, result.size());
        assertEquals(description, result.getLast().getDescription());
        verify(pollOptionRepository, times(1)).saveAndFlush(any(PollOption.class));
    }

    @Test
    void shouldRemovePollOption() {
        Long optionId = 5L;

        var examplePoll = Poll.builder().id(optionId).status(PollStatus.NOT_STARTED).build();

        when(pollOptionRepository.findById(optionId)).thenReturn(Optional.of(PollOption.builder().id(optionId).poll(examplePoll).build()));
        doNothing().when(pollOptionRepository).deleteById(optionId);
        when(pollOptionRepository.findAllByPollId(any())).thenReturn(List.of(PollOption.builder().build(),PollOption.builder().build(),PollOption.builder().build(),PollOption.builder().build()));

        optionService.removePollOption(optionId);

        verify(pollOptionRepository, times(1)).deleteById(optionId);
    }

    @Test
    void shouldReturnPollOptionsDTOs() {
        Long pollId = 1L;

        PollOption option1 = PollOption.builder()
                .id(1L)
                .description("Opção A")
                .poll(Poll.builder().id(pollId).build())
                .build();

        PollOption option2 = PollOption.builder()
                .id(2L)
                .description("Opção B")
                .poll(Poll.builder().id(pollId).build())
                .build();

        when(pollOptionRepository.findAllByPollId(pollId))
                .thenReturn(List.of(option1, option2));

        List<PollOptionDTO> result = optionService.getPollOptionsDTOS(pollId);

        assertEquals(2, result.size());
        assertEquals("Opção A", result.get(0).getDescription());
        assertEquals("Opção B", result.get(1).getDescription());
    }

    @Test
    void shouldAddVoteWhenPollIsInProgress() {
        Long optionId = 1L;
        Poll poll = Poll.builder()
                .id(100L)
                .status(PollStatus.STARTED)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        PollOption option = PollOption.builder()
                .id(optionId)
                .description("Opção A")
                .votes(5)
                .poll(poll)
                .build();

        // Mocka o repositório para retornar a opção existente
        when(pollOptionRepository.findById(optionId)).thenReturn(Optional.of(option));
        // Mocka o save para retornar a opção com votos incrementados
        when(pollOptionRepository.saveAndFlush(any(PollOption.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(pollOptionRepository.incrementVote(any())).thenReturn(1);

        PollOptionDTO result = optionService.voteForOption(optionId);

        assertNotNull(result);
        //update happens on the repository so don't check it here
//        assertEquals(6, result.votes()); // voto incrementado
        assertEquals("Opção A", result.getDescription());
        verify(pollOptionRepository, times(1)).incrementVote(any());
    }

    @Test
    void shouldThrowExceptionWhenPollIsNotInProgress() {
        Long optionId = 2L;
        Poll poll = Poll.builder()
                .id(200L)
                .status(PollStatus.NOT_STARTED) // ❌ não está em andamento
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        PollOption option = PollOption.builder()
                .id(optionId)
                .description("Opção B")
                .votes(0)
                .poll(poll)
                .build();

        when(pollOptionRepository.findById(optionId)).thenReturn(Optional.of(option));

        assertThrows(InvalidPollUpdateException.class,
                () -> optionService.voteForOption(optionId));

        verify(pollOptionRepository, never()).saveAndFlush(any(PollOption.class));
    }

    @Test
    void shouldThrowExceptionWhenOptionNotFound() {
        Long optionId = 999L;

        when(pollOptionRepository.findById(optionId)).thenReturn(Optional.empty());

        assertThrows(InvalidPollUpdateException.class,
                () -> optionService.voteForOption(optionId));

        verify(pollOptionRepository, never()).saveAndFlush(any(PollOption.class));
    }

}
