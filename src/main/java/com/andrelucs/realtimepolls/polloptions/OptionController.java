package com.andrelucs.realtimepolls.polloptions;

import com.andrelucs.realtimepolls.polls.PollService;
import com.andrelucs.realtimepolls.data.dto.PollOptionDTO;
import com.andrelucs.realtimepolls.exceptions.controller.BadRequestException;
import com.andrelucs.realtimepolls.exceptions.controller.FailPollOptionsUpdateException;
import com.andrelucs.realtimepolls.exceptions.controller.PollNotFoundException;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollUpdateException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/poll/{poll_id}/options")
public class OptionController {

    private final OptionService optionService;
    private final PollService pollService;

    public OptionController(OptionService optionService, PollService pollService) {
        this.optionService = optionService;
        this.pollService = pollService;
    }

    @PatchMapping()
    List<PollOptionDTO> addPollOption(@PathVariable Long poll_id,
                                      @RequestParam(name = "description") String description){
        if (description.isBlank()) {
            throw new BadRequestException("The Option description cannot be blank.");
        }
        if (!pollService.pollExists(poll_id)){
            throw new PollNotFoundException("Poll was not found.");
        }
        try{
            return optionService.addPollOption(poll_id, description);
        } catch (InvalidPollUpdateException e){
            throw new FailPollOptionsUpdateException("Failed to update poll options on poll %d. Reason: %s".formatted(poll_id, e.getMessage()));
        }

    }

    @DeleteMapping("/{option_id}")
    List<PollOptionDTO> removePollOption(@PathVariable Long poll_id, @PathVariable Long option_id) {
        if (option_id == null) {
            throw new BadRequestException("The Option id cannot be blank.");
        }
        if (!pollService.pollExists(poll_id)){
            throw new PollNotFoundException("Poll was not found.");
        }
        try{
            return optionService.removePollOption(option_id);
        } catch (InvalidPollUpdateException e){
            throw new FailPollOptionsUpdateException("Failed to update poll options on poll %d. Reason: %s".formatted(poll_id, e.getMessage()));
        }
    }
}
