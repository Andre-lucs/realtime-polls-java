package com.andrelucs.realtimepolls.websocket;

import com.andrelucs.realtimepolls.polloptions.PollOptionRepository;
import com.andrelucs.realtimepolls.websocket.data.PollOptionVoteDTO;
import com.andrelucs.realtimepolls.websocket.events.PollStatusEvent;
import com.andrelucs.realtimepolls.websocket.events.PollVoteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PollWsNotifier {

    private static final Logger log = LoggerFactory.getLogger(PollWsNotifier.class);
    private final SimpMessagingTemplate template;
    private final PollOptionRepository optionRepository;

    public PollWsNotifier(SimpMessagingTemplate template, PollOptionRepository optionRepository) {
        this.template = template;
        this.optionRepository = optionRepository;
    }

    // /topic/poll.{pollId}.votes
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPollOptionVote(PollVoteEvent voteEvent){
        var optionId = voteEvent.getData().getOptionId();
        var option = optionRepository.getReferenceById(optionId);
        var data = voteEvent.getData();
        PollOptionVoteDTO dto = PollOptionVoteDTO.builder()
                .pollId(data.getPollId())
                .optionId(optionId)
                .votes((long) option.getVotes())
                .delta(data.getDelta())
                .timestamp(data.getTimestamp())
                .build();
        template.convertAndSend("/topic/poll.%d.votes".formatted(data.getPollId()), dto);
    }

    // /topic/poll.{pollId}.status
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPollStatusEvent(PollStatusEvent statusEvent){

        var payload = statusEvent.getData();
        String topicUrl = "/topic/poll.%d.status".formatted(payload.getPollId());
        template.convertAndSend(topicUrl, payload);
    }

}
