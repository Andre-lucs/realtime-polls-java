package com.andrelucs.realtimepolls.polloptions;

import com.andrelucs.realtimepolls.data.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findAllByPollId(Long pollId);

    boolean existsByPollIdAndDescription(Long pollId, String description);

    @Modifying
    @Query("UPDATE PollOption o SET o.votes = o.votes + 1 WHERE o.id = :optionId")
    int incrementVote(@Param("optionId") Long optionId);
}
