package com.andrelucs.realtimepolls.integrationtests;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.polls.PollRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PollRepositoryTests extends AbstractIntegrationTest{

    final PollRepository pollRepository;

    @Autowired
    public PollRepositoryTests(PollRepository pollRepository) {
        super(pollRepository);
    }

    @Test
    @Transactional
    void shouldFindAllPolls() throws Exception {

        var result = pollRepository.findAll();

        Assertions.assertEquals(4, result.size());

        log.info(result.toString());
    }
}
