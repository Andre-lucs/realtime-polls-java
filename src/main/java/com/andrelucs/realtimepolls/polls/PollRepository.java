package com.andrelucs.realtimepolls.polls;

import com.andrelucs.realtimepolls.data.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRepository extends JpaRepository<Poll, Long> {
}
