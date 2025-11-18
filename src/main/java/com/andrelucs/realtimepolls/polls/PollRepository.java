package com.andrelucs.realtimepolls.polls;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findAllByStatus(PollStatus status);

    @Query("""
    SELECT p
    FROM Poll p
    LEFT JOIN FETCH p.options
    WHERE p.id = :id
    """)
    Optional<Poll> findFullPollById(@Param("id") Long id);


    @Modifying
    @Query("""
    UPDATE Poll p
    SET p.status = CASE
        WHEN CURRENT_TIMESTAMP < p.startDate THEN com.andrelucs.realtimepolls.data.model.PollStatus.NOT_STARTED
        WHEN CURRENT_TIMESTAMP >= p.startDate AND CURRENT_TIMESTAMP < p.endDate THEN com.andrelucs.realtimepolls.data.model.PollStatus.STARTED
        ELSE com.andrelucs.realtimepolls.data.model.PollStatus.FINISHED
    END
    WHERE p.id = :id
    """)
    int recalculateStatusById(@Param("id") Long id);

    @Modifying
    @Query("""
    UPDATE Poll p
    SET p.status = CASE
        WHEN CURRENT_TIMESTAMP < p.startDate THEN com.andrelucs.realtimepolls.data.model.PollStatus.NOT_STARTED
        WHEN CURRENT_TIMESTAMP >= p.startDate AND CURRENT_TIMESTAMP < p.endDate THEN com.andrelucs.realtimepolls.data.model.PollStatus.STARTED
        ELSE com.andrelucs.realtimepolls.data.model.PollStatus.FINISHED
    END
    WHERE p.status <> CASE
        WHEN CURRENT_TIMESTAMP < p.startDate THEN com.andrelucs.realtimepolls.data.model.PollStatus.NOT_STARTED
        WHEN CURRENT_TIMESTAMP >= p.startDate AND CURRENT_TIMESTAMP < p.endDate THEN com.andrelucs.realtimepolls.data.model.PollStatus.STARTED
        ELSE com.andrelucs.realtimepolls.data.model.PollStatus.FINISHED
    END
    """)
    int recalculateAllStatuses();


}
