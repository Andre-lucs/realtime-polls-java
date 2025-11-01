package com.andrelucs.realtimepolls.polls;

import com.andrelucs.realtimepolls.data.dto.PollDTO;
import com.andrelucs.realtimepolls.data.dto.PollRequestDTO;
import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollCreationException;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollEditException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PollService{

    private final PollRepository repository;

    private final ModelMapper modelMapper;

    public PollService(PollRepository repository, ModelMapper modelMapper) {
        this.repository = repository;
        this.modelMapper = modelMapper;
    }

    public List<PollDTO> findAll(){
        return repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<PollDTO> findByStatus(PollStatus status) {
        return null;
    }

    public Optional<PollDTO> findById(Long pollId) {
        return Optional.empty();
    }

    public PollDTO save(PollRequestDTO poll) throws InvalidPollCreationException {
        return null;
    }

    private PollDTO convertToDTO(Poll poll) {
        return modelMapper.map(poll, PollDTO.class);
    }

    private Poll convertToPoll(PollDTO pollDTO) {
        return modelMapper.map(pollDTO, Poll.class);
    }

    public boolean pollExists(Long pollId) {
        return false;
    }



    public void deletePoll(Long pollId) {

    }

    public PollDTO editPoll(Long poll_id, PollRequestDTO newPollObject) throws InvalidPollEditException {
        return null;
    }
}
