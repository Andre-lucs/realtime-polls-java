package com.andrelucs.realtimepolls.polls.scheduler;

import com.andrelucs.realtimepolls.data.PostgresNotificationListener;
import com.andrelucs.realtimepolls.data.model.StatusToUpdate;
import com.andrelucs.realtimepolls.polls.PollService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class SmartStatusScheduler {

    private record NewSchedulePayload(long id, LocalDateTime dateTime){
        static final String dateTimePartern =  "yyyy-MM-dd'T'HH:mm:ss";
        static DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimePartern);
        private NewSchedulePayload(long id, String dateString) {
            this(id, LocalDateTime.parse(trimToPaternLength(dateString), formatter));
        }

        static private String trimToPaternLength(String dateTimeString){
            return dateTimeString.trim().substring(0, dateTimePartern.length()-2); // using -2 to not count the ' around the T
        }
    }

    private record StatusToUpdateTask(StatusToUpdate statusToUpdate, PollService pollService) implements Runnable {
        @Override
        public void run() {
            pollService.processStatus(statusToUpdate.getId());
        }
    }

    private static final String STATUS_UPDATE_CHANEL = "status_to_update_channel";

    private final PostgresNotificationListener notificationListener;
    private final PostgresNotificationListener.PayloadHandler notificationHandler;
    private final StatusToUpdateRepository statusToUpdateRepository;
    private final PollService pollService;
    private final TaskScheduler taskScheduler;

    private StatusToUpdateTask updateTask;
    private ScheduledFuture<?> scheduledUpdateTask;

    public SmartStatusScheduler(PostgresNotificationListener notificationListener, StatusToUpdateRepository statusToUpdateRepository, PollService pollService, TaskScheduler threadPollTaskScheduler) {
        this.notificationListener = notificationListener;
        this.statusToUpdateRepository = statusToUpdateRepository;
        this.pollService = pollService;
        this.taskScheduler = threadPollTaskScheduler;

        this.notificationHandler = this::handleNotificationPayload;
    }

    public synchronized void handleNotificationPayload(String payload){
        log.info("Received notification payload: {}", payload);
        var args = payload.split("\\|", 2);

        var event = new NewSchedulePayload(Long.parseLong(args[0]), args[1]);

        var statusToUpdate = getStatusBeingProcessed();

        boolean shouldChange = statusToUpdate == null || event.dateTime.isBefore(statusToUpdate.getScheduledDate());

        if (shouldChange){
            statusToUpdateRepository.findById(event.id()).ifPresent(this::processStatus);
        }
    }

    public StatusToUpdate getStatusBeingProcessed(){
        if (updateTask == null) return null;
        return updateTask.statusToUpdate;
    }

    public synchronized void processStatus(StatusToUpdate statusToUpdate){
        log.info("Changed Status being processed from {} to {}", getStatusBeingProcessed(), statusToUpdate );
        cancelScheduledTask();

        updateTask = new StatusToUpdateTask(statusToUpdate, pollService);

        scheduledUpdateTask = taskScheduler.schedule(
                () -> {
                    updateTask.run();
                    processMostRecentStatus(); // <- keep processing the next status
                },
                statusToUpdate.getScheduledDate().atZone(ZoneId.systemDefault()).toInstant()
        );
    }

    public void cancelScheduledTask() {
        if (scheduledUpdateTask != null) {
            scheduledUpdateTask.cancel(false);
            scheduledUpdateTask = null;
        }
        updateTask = null;
    }

    public void processMostRecentStatus(){
        // Ensure the ones in the past are already processed
        pollService.processStatusBefore(LocalDateTime.now());
        statusToUpdateRepository.findFirstNonProcessed().ifPresent(this::processStatus);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void afterPropertiesSet() {
        //subscribe to the channel
        ensureIsListening();
        //awaits most recent StatusToUpdate
        processMostRecentStatus();
    }

    public void ensureIsListening() {
        if (notificationListener.isListening(STATUS_UPDATE_CHANEL, notificationHandler)) return;

        try {
            notificationListener.listen(STATUS_UPDATE_CHANEL, notificationHandler);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
