package com.andrelucs.realtimepolls.polls;

import com.andrelucs.realtimepolls.data.dto.PollDTO;
import com.andrelucs.realtimepolls.data.dto.PollRequestDTO;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.exceptions.controller.BadRequestException;
import com.andrelucs.realtimepolls.exceptions.controller.InvalidPollException;
import com.andrelucs.realtimepolls.exceptions.controller.PollNotFoundException;
import com.andrelucs.realtimepolls.exceptions.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/poll")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping
    List<PollDTO> findAll(@RequestParam(required = false) PollStatus status){
        if (status == null) {
            return pollService.findAll();
        }
        return pollService.findByStatus(status);
    }

    @GetMapping("/{poll_id}")
    PollDTO findById(@PathVariable Long poll_id){
        Optional<PollDTO> poll = pollService.findById(poll_id);

        return poll.orElseThrow(() -> new PollNotFoundException("Poll was not found."));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PollDTO save(@RequestBody PollRequestDTO poll) throws InvalidPollException {
        try{
            PollDTO createdPoll = pollService.save(poll);
            if (createdPoll.id() == null) {
                throw new InvalidPollException("Poll was not created.");
            }
            return createdPoll;

        }catch (InvalidPollCreationException e){
            throw new InvalidPollException(e.getMessage());
        }
    }

    @PutMapping("/{poll_id}")
    PollDTO editPoll(@PathVariable Long poll_id, @RequestBody PollRequestDTO newPollObject) {
        try{
            PollDTO createdPoll = pollService.editPoll(poll_id, newPollObject);
            if (createdPoll.id() == null) {
                throw new InvalidPollException("Poll was not created.");
            }
            return createdPoll;

        }catch (InvalidPollEditException e){
            throw new InvalidPollException(e.getMessage());
        }
    }

    @DeleteMapping("/{poll_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deletePoll(@PathVariable Long poll_id){
        pollService.deletePoll(poll_id);
    }


}
