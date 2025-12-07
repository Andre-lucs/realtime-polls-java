package com.andrelucs.realtimepolls.unittests;

import com.andrelucs.realtimepolls.data.PostgresNotificationListener;
import com.andrelucs.realtimepolls.data.model.StatusToUpdate;
import com.andrelucs.realtimepolls.polls.PollService;
import com.andrelucs.realtimepolls.polls.scheduler.SmartStatusScheduler;
import com.andrelucs.realtimepolls.polls.scheduler.StatusToUpdateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SmartStatusSchedulerUnitTest {

    @Mock
    private PostgresNotificationListener listener;

    @Mock
    private StatusToUpdateRepository repository;

    @Mock
    private PollService pollService;

    @Mock
    private TaskScheduler scheduler;

    @Mock
    private ScheduledFuture future;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    private SmartStatusScheduler smart;

    @BeforeEach
    void setup() {
        smart = new SmartStatusScheduler(listener, repository, pollService, scheduler);
    }

    @Test
    void shouldScheduleTaskWhenProcessingStatus() {
        var date = LocalDateTime.now().plusSeconds(10);
        StatusToUpdate status = new StatusToUpdate();
        status.setScheduledDate(date);

        when(scheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(future);

        smart.processStatus(status);

        Assertions.assertEquals(status, smart.getStatusBeingProcessed());
        verify(scheduler).schedule(any(Runnable.class), eq(date.atZone(ZoneId.systemDefault()).toInstant()));
    }

    @Test
    void shouldCancelPreviousTaskWhenSchedulingNewOne() {
        var date1 = LocalDateTime.now().plusMinutes(10);
        var date2 = LocalDateTime.now().plusMinutes(5);

        StatusToUpdate s1 = new StatusToUpdate();
        s1.setScheduledDate(date1);

        StatusToUpdate s2 = new StatusToUpdate();
        s2.setScheduledDate(date2);

        when(scheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(future);

        smart.processStatus(s1);
        smart.processStatus(s2);

        verify(future).cancel(false);
    }

    @Test
    void shouldReplaceWithCloserEventOnNotification() {
        StatusToUpdate oldEvent = new StatusToUpdate();
        oldEvent.setId(10L);
        oldEvent.setScheduledDate(LocalDateTime.now().plusHours(6));

        StatusToUpdate newEvent = new StatusToUpdate();
        newEvent.setId(20L);
        newEvent.setScheduledDate(LocalDateTime.now().plusHours(1));

        smart.processStatus(oldEvent);

        when(repository.findById(20L)).thenReturn(Optional.of(newEvent));

        smart.handleNotificationPayload(
                "20|" + newEvent.getScheduledDate().toString()
        );

        Assertions.assertEquals(newEvent, smart.getStatusBeingProcessed());
    }

    @Test
    void shouldIgnoreNotificationForFartherEvent() {
        StatusToUpdate original = new StatusToUpdate();
        original.setId(1L);
        original.setScheduledDate(LocalDateTime.now().plusHours(1));

        StatusToUpdate farther = new StatusToUpdate();
        farther.setId(2L);
        farther.setScheduledDate(LocalDateTime.now().plusHours(5));

        smart.processStatus(original);

        smart.handleNotificationPayload(
                "2|" + farther.getScheduledDate().toString()
        );

        Assertions.assertEquals(original, smart.getStatusBeingProcessed());
    }

    @Test
    void scheduledTaskShouldCallPollService() {
        var date = LocalDateTime.now().plusSeconds(1);

        StatusToUpdate status = new StatusToUpdate();
        status.setScheduledDate(date);

        when(scheduler.schedule(runnableCaptor.capture(), any(Instant.class)))
                .thenReturn(future);

        smart.processStatus(status);

        Runnable task = runnableCaptor.getValue();

        task.run();

        verify(pollService).processStatus(status);
    }
}
