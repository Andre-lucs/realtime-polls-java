package com.andrelucs.realtimepolls.polloptions;

import com.andrelucs.realtimepolls.data.dto.PollOptionDTO;
import com.andrelucs.realtimepolls.exceptions.service.InvalidPollUpdateException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OptionService {

    /**
     * Adds a new option to the desired Poll
     * @param pollId
     * @param description
     * @return The new list of polls
     * @throws InvalidPollUpdateException Thrown when trying to add to a Poll already started or when the option already exists
     */
    public List<PollOptionDTO> addPollOption(Long pollId, String description) throws InvalidPollUpdateException {
        return null;
    }

    public List<PollOptionDTO> removePollOption(Long optionId) throws InvalidPollUpdateException {
        return null;
    }

}
