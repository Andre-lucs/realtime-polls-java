package com.andrelucs.realtimepolls.polloptions;

import com.andrelucs.realtimepolls.data.dto.PollOptionDTO;
import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollUpdateException;
import com.andrelucs.realtimepolls.polls.PollRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OptionService {

    private static final Logger log = LoggerFactory.getLogger(OptionService.class);
    private final PollOptionRepository optionRepository;
    private final PollRepository pollRepository;
    private final ModelMapper modelMapper;

    public OptionService(PollOptionRepository optionRepository, PollRepository pollRepository, ModelMapper modelMapper) {
        this.optionRepository = optionRepository;
        this.pollRepository = pollRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Adds a new option to the desired Poll
     * @param pollId the Poll's id to add a option
     * @param description option description
     * @return The new list of options
     * @throws InvalidPollUpdateException Thrown when trying to add to a Poll already started or when the option already exists
     */
    public List<PollOptionDTO> addPollOption(Long pollId, String description) throws InvalidPollUpdateException {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new InvalidPollUpdateException("Poll not found"));

        if (poll.getStatus() != PollStatus.NOT_STARTED) {
            throw new InvalidPollUpdateException("Cannot add options to a poll that has already started");
        }

        boolean exists = optionRepository.existsByPollIdAndDescription(pollId, description);

        if (exists) {
            throw new InvalidPollUpdateException("Option already exists in this poll");
        }

        PollOption option = PollOption.builder()
                .description(description)
                .votes(0)
                .poll(poll)
                .build();

        optionRepository.saveAndFlush(option);

        log.info("Added option '{}' to poll {}", description, pollId);
        return getPollOptionsDTOS(pollId);
    }

    /**
     * Removes an option by its id
     * @param optionId The option's DB ID
     * @throws InvalidPollUpdateException When the total options would be less than 3 options
     */
    public void removePollOption(Long optionId) throws InvalidPollUpdateException {
        PollOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new InvalidPollUpdateException("Option not found"));

        Poll poll = option.getPoll();
        if (poll.getStatus() != PollStatus.NOT_STARTED) {
            throw new InvalidPollUpdateException("Cannot remove options from a poll that has already started");
        }

        List<PollOption> options = optionRepository.findAllByPollId(poll.getId());
        if (options.size() <= 3) {
            throw new InvalidPollUpdateException("Poll must have at least 3 options");
        }

        optionRepository.deleteById(optionId);
        log.info("Removed option {} from poll {}", optionId, poll.getId());
    }

    /**
     * Adds a vote to an option
     * @param optionId the option id to add a vote
     * @return The updated PollOption
     * @throws InvalidPollUpdateException when the poll is not in progress
     */
    @Transactional
    public PollOptionDTO voteForOption(Long optionId) throws InvalidPollUpdateException {
        PollOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new InvalidPollUpdateException("Option not found"));

        Poll poll = option.getPoll();
        if (poll.getStatus() != PollStatus.STARTED) {
            throw new InvalidPollUpdateException("Poll is not in progress, cannot vote");
        }

        int updated = optionRepository.incrementVote(optionId);
        if (updated == 0) {
            throw new InvalidPollUpdateException("Vote could not be registered");
        }

        // recarrega a opção atualizada
        PollOption updatedOption = optionRepository.findById(optionId)
                .orElseThrow(() -> new InvalidPollUpdateException("Option not found after update"));

        return modelMapper.map(updatedOption, PollOptionDTO.class);
    }


    public List<PollOptionDTO> getPollOptionsDTOS(Long pollId) {
        return getPollOptions(pollId)
                .stream()
                .map(o -> modelMapper.map(o, PollOptionDTO.class))
                .toList();
    }

    private List<PollOption> getPollOptions(Long pollId) {
        return optionRepository.findAllByPollId(pollId);
    }


}
