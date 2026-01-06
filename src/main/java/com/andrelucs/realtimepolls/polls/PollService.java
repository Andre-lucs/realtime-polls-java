package com.andrelucs.realtimepolls.polls;

import com.andrelucs.realtimepolls.data.dto.PollDTO;
import com.andrelucs.realtimepolls.data.dto.PollEditRequestDTO;
import com.andrelucs.realtimepolls.data.dto.PollRequestDTO;
import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.data.model.StatusToUpdate;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollCreationException;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollEditException;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollEntityException;
import com.andrelucs.realtimepolls.polls.scheduler.StatusToUpdateRepository;
import com.andrelucs.realtimepolls.websocket.data.PollStatusUpdateDTO;
import com.andrelucs.realtimepolls.websocket.events.PollStatusEvent;
import jakarta.transaction.Transactional;
import jakarta.validation.*;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PollService{

    private static final Logger log = LoggerFactory.getLogger(PollService.class);
    private final PollRepository repository;
    private final StatusToUpdateRepository statusToUpdateRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;


    private final Validator validator;
    private final PollRepository pollRepository;

    public PollService(PollRepository repository, StatusToUpdateRepository statusToUpdateRepository, ModelMapper modelMapper, ApplicationEventPublisher eventPublisher, PollRepository pollRepository) {
        this.repository = repository;
        this.statusToUpdateRepository = statusToUpdateRepository;
        this.modelMapper = modelMapper;
        this.eventPublisher = eventPublisher;
        var validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
        this.pollRepository = pollRepository;
    }

    public List<PollDTO> findAll(){
        return repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<PollDTO> findByStatus(PollStatus status) {
        return repository.findAllByStatus(status)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Transactional
    public Optional<PollDTO> findById(Long pollId) {
        return findPollEntity(pollId)
                .map(this::convertToDTO);
    }

    @Transactional
    public Optional<Poll> findPollEntity(Long pollId) {
        // Updating the status to make sure we get the correct status by the time
        repository.recalculateStatusById(pollId);
        return repository.findById(pollId);
    }

    public PollDTO save(PollRequestDTO poll) throws InvalidPollCreationException {

        Poll pollEntity = Poll.builder()
                .question(poll.question())
                .startDate(poll.startDate())
                .endDate(poll.endDate())
                .build();

        for (var option : poll.options()) {
            pollEntity.addOption(PollOption.builder().description(option).build());
        }

        try {
            validatePollEntity(pollEntity);
        } catch (InvalidPollEntityException e) {
            throw new InvalidPollCreationException(e.getMessage());
        }

        var saved = repository.save(pollEntity);
        log.info("Saved poll is :{}", saved);

        return convertToDTO(saved);
    }


    public boolean pollExists(Long pollId) {
        return repository.existsById(pollId);
    }

    public void deletePoll(Long pollId) {
        repository.deleteById(pollId);
    }

    public PollDTO editPoll(Long poll_id, PollEditRequestDTO newPollObject) throws InvalidPollEditException {
        var poll = repository.findById(poll_id);
        // Validate poll existence
        if (poll.isEmpty()){
            throw new InvalidPollEditException("Poll not found");
        }

        var pollEntity = poll.get();

        if (pollEntity.getStatus() != PollStatus.NOT_STARTED){
            throw new InvalidPollEditException("Poll status is %s can only edit NOT_STARTED polls".formatted(pollEntity.getStatus()));
        }
        // Update fields
        pollEntity.setQuestion((newPollObject.question() != null && !newPollObject.question().isBlank()) ? newPollObject.question() : pollEntity.getQuestion() );
        pollEntity.setStartDate((newPollObject.startDate() != null) ? newPollObject.startDate() : pollEntity.getStartDate());
        pollEntity.setEndDate((newPollObject.endDate() != null) ? newPollObject.endDate() : pollEntity.getEndDate());

        // Validate entity
        try {
            validatePollEntity(pollEntity);
        } catch (InvalidPollEntityException e) {
            throw new InvalidPollEditException(e.getMessage());
        }

        var edited = repository.save(pollEntity);
        return convertToDTO(edited);
    }

    private PollDTO convertToDTO(Poll poll) {
        return modelMapper.map(poll, PollDTO.class);
    }

    private Poll convertToPoll(PollDTO pollDTO) {
        return modelMapper.map(pollDTO, Poll.class);
    }

    // Changing to use new status update system
//    @Scheduled(cron = "0 */10 * * * *")
//    public void recalculateStatuses() {
//        repository.recalculateAllStatuses();
//    }

    private void validatePollEntity(Poll pollEntity) throws InvalidPollEntityException {

        StringBuilder violationMessage = new StringBuilder();
        AtomicInteger i = new AtomicInteger();

        if (pollEntity.getStartDate().isAfter(pollEntity.getEndDate())){
            violationMessage.append("%d: ".formatted(i.getAndIncrement()));
            violationMessage.append("The poll start date cannot be after the end date");
        }

        var violations = validator.validate(pollEntity);

        if (!violations.isEmpty()){
            violations.forEach(violation ->
                    violationMessage.append("%d: ".formatted(i.getAndIncrement())).append(violation.getMessage())
            );
            log.info(violationMessage.toString());
        }
        if (violationMessage.isEmpty()) return;

        throw new InvalidPollEntityException(violationMessage.toString());
    }

    @Transactional
    public void processStatusBefore(LocalDateTime date) {
        var statusEvents = statusToUpdateRepository.findNonProcessedBefore(date);

        for (StatusToUpdate statusEvent : statusEvents) {
            var poll = statusEvent.getPoll();

            poll.setStatus(statusEvent.getNextStatus());
            statusEvent.setProcessedAt(LocalDateTime.now());
        }

        pollRepository.flush();
        statusToUpdateRepository.flush();
    }

    @Transactional
    public void processStatus(Long statusToUpdateId) {
        var statusToUpdate = statusToUpdateRepository.findByIdWithPoll(statusToUpdateId).orElseThrow();
        var poll = statusToUpdate.getPoll();
        poll.setStatus(statusToUpdate.getNextStatus());
        statusToUpdate.setProcessedAt(LocalDateTime.now());

        pollRepository.flush();
        statusToUpdateRepository.flush();

        var event = new PollStatusEvent(this, PollStatusUpdateDTO.builder()
                .pollId(statusToUpdate.getPoll().getId())
                .timestamp(LocalDateTime.now())
                .fromStatus(statusToUpdate.getCurrentStatus())
                .toStatus(statusToUpdate.getNextStatus())
                .build());
        eventPublisher.publishEvent(event);
    }
}
