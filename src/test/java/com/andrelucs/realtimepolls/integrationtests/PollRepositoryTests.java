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
        this.pollRepository = pollRepository;
    }

    @AfterEach
    void setUp() {
        pollRepository.deleteAll();
    }

    /**
     * Saves a set os default polls to the database
     */
    @BeforeEach
    void saveTestData(){
        var polls = List.of(
                Poll.builder()
                        .question("Qual sua linguagem de programação favorita?")
                        .startDate(LocalDateTime.now().plusDays(1))
                        .endDate(LocalDateTime.now().plusDays(5))
                        .options(new ArrayList<>())
                        .build(),

                Poll.builder()
                        .question("Qual seu sistema operacional preferido?")
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(3))
                        .options(new ArrayList<>())
                        .build(),

                Poll.builder()
                        .question("Qual banco de dados você mais utiliza?")
                        .startDate(LocalDateTime.now().minusDays(2))
                        .endDate(LocalDateTime.now().plusDays(2))
                        .options(new ArrayList<>())
                        .build(),

                Poll.builder()
                        .question("Qual framework web você prefere?")
                        .startDate(LocalDateTime.now().minusDays(10))
                        .endDate(LocalDateTime.now().minusDays(5))
                        .options(new ArrayList<>())
                        .build()
        );

        int pollI = 0;
        for (Poll poll : polls) {
            var options = poll.getOptions();
            pollI++;
            if (pollI == 1) {
                options.add(new PollOption(null, "Java", 0, poll));
                options.add(new PollOption(null, "Python", 0, poll));
                options.add(new PollOption(null, "JavaScript", 0, poll));
            } else if (pollI == 2) {
                options.add(new PollOption(null, "Windows", 5, poll));
                options.add(new PollOption(null, "Linux", 8, poll));
                options.add(new PollOption(null, "macOS", 2, poll));
            } else if (pollI == 3) {
                options.add(new PollOption(null, "PostgreSQL", 3, poll));
                options.add(new PollOption(null, "MySQL", 4, poll));
                options.add(new PollOption(null, "MongoDB", 1, poll));
            } else if (pollI == 4) {
                options.add(new PollOption(null, "Spring Boot", 10, poll));
                options.add(new PollOption(null, "Django", 6, poll));
                options.add(new PollOption(null, "Express.js", 4, poll));
            }
        }

        pollRepository.saveAll(polls);

    }


    @Test
    @Transactional
    void shouldFindAllPolls() throws Exception {

        var result = pollRepository.findAll();

        Assertions.assertEquals(4, result.size());

        log.info(result.toString());
    }
}
