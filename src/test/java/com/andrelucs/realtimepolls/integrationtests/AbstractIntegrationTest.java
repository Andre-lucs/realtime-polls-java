package com.andrelucs.realtimepolls.integrationtests;

import com.andrelucs.realtimepolls.data.model.Poll;
import com.andrelucs.realtimepolls.data.model.PollOption;
import com.andrelucs.realtimepolls.polls.PollRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test-containers")
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("polldb")
            .withUsername("postgres")
            .withPassword("pass");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    // Test data setup ---

    final PollRepository pollRepository;

    public AbstractIntegrationTest(PollRepository pollRepository) {
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
}
