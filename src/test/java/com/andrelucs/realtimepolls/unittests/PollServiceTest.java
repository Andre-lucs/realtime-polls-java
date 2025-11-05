package com.andrelucs.realtimepolls.unittests;

import com.andrelucs.realtimepolls.config.ModelMapperConfiguration;
import com.andrelucs.realtimepolls.data.dto.PollDTO;
import com.andrelucs.realtimepolls.data.dto.PollEditRequestDTO;
import com.andrelucs.realtimepolls.data.dto.PollRequestDTO;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollCreationException;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollEditException;
import com.andrelucs.realtimepolls.polls.PollRepository;
import com.andrelucs.realtimepolls.polls.PollService;
import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollStatus;
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
@ContextConfiguration(classes = {PollService.class, ModelMapperConfiguration.class})
@Slf4j
public class PollServiceTest {

    @MockitoBean
    PollRepository pollRepository;

    @Autowired
    PollService service;

    List<Poll> testPolls;

    @BeforeEach
    void setUp() {
        reset();

        testPolls = List.of(
                Poll.builder()
                        .id(1L)
                        .question("Qual sua linguagem de programação favorita?")
                        .status(PollStatus.NOT_STARTED)
                        .startDate(LocalDateTime.now().plusDays(1))
                        .endDate(LocalDateTime.now().plusDays(5))
                        .options(new ArrayList<>())
                        .build(),

                Poll.builder()
                        .id(2L)
                        .question("Qual seu sistema operacional preferido?")
                        .status(PollStatus.STARTED)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(3))
                        .options(new ArrayList<>())
                        .build(),

                Poll.builder()
                        .id(3L)
                        .question("Qual banco de dados você mais utiliza?")
                        .status(PollStatus.STARTED)
                        .startDate(LocalDateTime.now().minusDays(2))
                        .endDate(LocalDateTime.now().plusDays(2))
                        .options(new ArrayList<>())
                        .build(),

                Poll.builder()
                        .id(4L)
                        .question("Qual framework web você prefere?")
                        .status(PollStatus.FINISHED)
                        .startDate(LocalDateTime.now().minusDays(10))
                        .endDate(LocalDateTime.now().minusDays(5))
                        .options(new ArrayList<>())
                        .build()
        );

        var optionId = 1L;
        for (Poll poll : testPolls) {
            var options = poll.getOptions();

            if (poll.getId() == 1L) {
                options.add(new PollOption(optionId++, "Java", 0, poll));
                options.add(new PollOption(optionId++, "Python", 0, poll));
                options.add(new PollOption(optionId++, "JavaScript", 0, poll));
            } else if (poll.getId() == 2L) {
                options.add(new PollOption(optionId++, "Windows", 5, poll));
                options.add(new PollOption(optionId++, "Linux", 8, poll));
                options.add(new PollOption(optionId++, "macOS", 2, poll));
            } else if (poll.getId() == 3L) {
                options.add(new PollOption(optionId++, "PostgreSQL", 3, poll));
                options.add(new PollOption(optionId++, "MySQL", 4, poll));
                options.add(new PollOption(optionId++, "MongoDB", 1, poll));
            } else if (poll.getId() == 4L) {
                options.add(new PollOption(optionId++, "Spring Boot", 10, poll));
                options.add(new PollOption(optionId++, "Django", 6, poll));
                options.add(new PollOption(optionId++, "Express.js", 4, poll));
            }
        }
    }

    @Test
    void shouldReturnAllPolls() {
        when(pollRepository.findAll()).thenReturn(testPolls);

        List<PollDTO> result = service.findAll();

        assertEquals(result.size(), testPolls.size());

        for (var poll : result){
            Poll respectivePoll = testPolls.stream().filter((p -> p.getId().equals(poll.getId()))).toList().stream().findFirst().orElse(null);
            assertNotNull(respectivePoll);
            assertEquals(poll.getQuestion(), respectivePoll.getQuestion());
            assertEquals(poll.getStartDate(), respectivePoll.getStartDate());
            assertEquals(poll.getEndDate(), respectivePoll.getEndDate());
            assertEquals(poll.getStatus(), respectivePoll.getStatus());
            assertEquals(poll.getOptions().size(), respectivePoll.getOptions().size());
        }

        log.info(result.toString());

    }

    @Test
    void shouldReturnPollsByStatus() {
        when(pollRepository.findAllByStatus(PollStatus.STARTED)).thenReturn(List.of(testPolls.get(1)));

        List<PollDTO> result = service.findByStatus(PollStatus.STARTED);

        assertEquals(1, result.size());
        assertEquals(PollStatus.STARTED, result.getFirst().getStatus());
    }

    @Test
    void shouldReturnEmptyListIfNotMatchedStatus() {
        when(pollRepository.findAllByStatus(PollStatus.STARTED)).thenReturn(List.of());
        List<PollDTO> result = service.findByStatus(PollStatus.STARTED);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void shouldReturnPollById() {
        Poll poll = testPolls.getFirst();
        when(pollRepository.findById(1L)).thenReturn(Optional.of(poll));

        Optional<PollDTO> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(poll.getId(), result.get().getId());
        assertEquals(poll.getQuestion(), result.get().getQuestion());
    }

    @Test
    void shouldReturnEmptyWhenPollNotFound() {
        when(pollRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<PollDTO> result = service.findById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSaveValidPoll() throws InvalidPollCreationException {
        var request = new PollRequestDTO(
                "Qual IDE você prefere?",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                List.of("IntelliJ", "Eclipse", "VS Code")
        );

        Poll pollEntity = Poll.builder()
                .id(10L)
                .question(request.question())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(PollStatus.NOT_STARTED)
                .options(new ArrayList<>())
                .build();

        pollEntity.getOptions().add(new PollOption(20L, "IntelliJ", 0, pollEntity));
        pollEntity.getOptions().add(new PollOption(21L, "Eclipse", 0, pollEntity));
        pollEntity.getOptions().add(new PollOption(22L, "VS Code", 0, pollEntity));
        // Should save along with the options
        when(pollRepository.save(any(Poll.class))).thenReturn(pollEntity);

        PollDTO result = service.save(request);

        assertNotNull(result);
        assertEquals("Qual IDE você prefere?", result.getQuestion());
        assertEquals(3, result.getOptions().size());
    }

    @Test
    void shouldNotSaveAPollWithInvalidDateRange() {
        var request = new PollRequestDTO(
                "Qual IDE você prefere?",
                LocalDateTime.now().plusDays(2), // Is after the endDate
                LocalDateTime.now().minusDays(1), // Is before the startDate
                List.of("IntelliJ", "Eclipse", "VS Code")
        );

        assertThrows(InvalidPollCreationException.class, () -> service.save(request));
    }

    @Test
    void shouldThrowExceptionWhenPollHasLessThanThreeOptions() {
        PollRequestDTO request = new PollRequestDTO(
                "Pergunta inválida",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                List.of("Opção 1", "Opção 2") // só 2 opções
        );

        assertThrows(InvalidPollCreationException.class, () -> service.save(request));
    }

    @Test
    void shouldReturnTrueWhenPollExists() {
        when(pollRepository.existsById(1L)).thenReturn(true);

        assertTrue(service.pollExists(1L));
    }

    @Test
    void shouldReturnFalseWhenPollDoesNotExist() {
        when(pollRepository.existsById(99L)).thenReturn(false);

        assertFalse(service.pollExists(99L));
    }

    @Test
    void shouldDeletePoll() {
        Long pollId = 1L;

        service.deletePoll(pollId);

        verify(pollRepository, times(1)).deleteById(pollId);
    }

    @Test
    void shouldEditPollWhenNotStarted() throws InvalidPollEditException {
        Poll existingPoll = testPolls.getFirst(); // NOT_STARTED
        when(pollRepository.findById(existingPoll.getId())).thenReturn(Optional.of(existingPoll));

        var newData = new PollEditRequestDTO(
                "Pergunta editada",
                existingPoll.getStartDate(),
                existingPoll.getEndDate()
        );

        existingPoll.setQuestion("Pergunta editada");
        when(pollRepository.save(any(Poll.class))).thenReturn(existingPoll);

        PollDTO result = service.editPoll(existingPoll.getId(), newData);

        assertEquals("Pergunta editada", result.getQuestion());
    }
    @Test
    void shouldThrowExceptionWhenEditingStartedOrFurtherPoll() {
        Poll startedPoll = testPolls.get(1); // STARTED
        when(pollRepository.findById(startedPoll.getId())).thenReturn(Optional.of(startedPoll));

        var newData = new PollEditRequestDTO(
                "Tentativa de edição",
                startedPoll.getStartDate(),
                startedPoll.getEndDate()
        );

        assertThrows(InvalidPollEditException.class,
                () -> service.editPoll(startedPoll.getId(), newData));
    }
    @Test
    void shouldEditOnlySpecifiedFields() throws InvalidPollEditException {
        // Arrange
        Poll existingPoll = testPolls.getFirst(); // NOT_STARTED
        existingPoll.setQuestion("Pergunta original");
        LocalDateTime originalStart = existingPoll.getStartDate();
        LocalDateTime originalEnd = existingPoll.getEndDate();
        List<PollOption> originalOptions = new ArrayList<>(existingPoll.getOptions());

        when(pollRepository.findById(existingPoll.getId())).thenReturn(Optional.of(existingPoll));

        // Apenas o campo "question" será alterado
        var updateRequest = new PollEditRequestDTO(
                "Pergunta alterada",
                null, // não altera startDate
                null // não altera endDate
        );

        // Simula que o repositório salva e retorna a entidade atualizada
        Poll updatedPoll = Poll.builder()
                .id(existingPoll.getId())
                .question(updateRequest.question())
                .startDate(originalStart)
                .endDate(originalEnd)
                .status(existingPoll.getStatus())
                .options(originalOptions)
                .build();

        when(pollRepository.save(any(Poll.class))).thenReturn(updatedPoll);

        // Act
        PollDTO result = service.editPoll(existingPoll.getId(), updateRequest);

        // Assert
        assertEquals("Pergunta alterada", result.getQuestion(), "O campo question deve ser atualizado");
        assertEquals(originalStart, result.getStartDate(), "O campo startDate não deve ser alterado");
        assertEquals(originalEnd, result.getEndDate(), "O campo endDate não deve ser alterado");
        assertEquals(originalOptions.size(), result.getOptions().size(), "As opções não devem ser alteradas");
    }




}
