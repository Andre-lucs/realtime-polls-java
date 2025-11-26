package com.andrelucs.realtimepolls.polls.scheduler;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import com.andrelucs.realtimepolls.data.model.StatusToUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StatusToUpdateRepository extends JpaRepository<StatusToUpdate, Long> {

    @Query("""
    select s
    from StatusToUpdate s
    order by s.scheduledDate
    limit :amount
    """)
    Set<StatusToUpdate> getFirstNonProcessedStatusToUpdate(int amount);

    default Optional<StatusToUpdate> findFirstNonProcessedStatusToUpdate(){
        return getFirstNonProcessedStatusToUpdate(1).stream().findFirst();
    };

    List<StatusToUpdate> findAllByNextStatus(PollStatus status);

    List<StatusToUpdate> findAllByPoll(Poll poll);

}
