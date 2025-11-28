package com.andrelucs.realtimepolls.polls.scheduler;

import com.andrelucs.realtimepolls.data.PostgresNotificationListener;
import com.andrelucs.realtimepolls.data.model.StatusToUpdate;
import com.andrelucs.realtimepolls.polls.PollService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class SmartStatusScheduler {

    private record NewSchedulePayload(long id, LocalDateTime dateTime){
        static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        private NewSchedulePayload(long id, String dateString) {
            this(id, LocalDateTime.parse(dateString, formatter));
        }
    }

    private static final String STATUS_UPDATE_CHANEL = "status_to_update_channel";

    private final PostgresNotificationListener notificationListener;
    private final PostgresNotificationListener.PayloadHandler notificationHandler;
    private final StatusToUpdateRepository statusToUpdateRepository;
    private final PollService pollService;

    private StatusToUpdate statusToUpdate;

    public SmartStatusScheduler(PostgresNotificationListener notificationListener, StatusToUpdateRepository statusToUpdateRepository, PollService pollService) {
        this.notificationListener = notificationListener;
        this.statusToUpdateRepository = statusToUpdateRepository;
        this.pollService = pollService;

        this.notificationHandler = this::handleNotificationPayload;
    }

    public synchronized void handleNotificationPayload(String payload){
        log.info("Received notification payload: {}", payload);
        var args = payload.split("\\|", 2);

        var event = new NewSchedulePayload(Long.parseLong(args[0]), args[1]);

        boolean shouldChange = statusToUpdate == null || event.dateTime.isBefore(statusToUpdate.getScheduledDate());

        if (shouldChange){
            var status = statusToUpdateRepository.findById(event.id())
                    .orElse(this.statusToUpdate); // If Not found keep the same status
            processStatus(status);
        }
    }

    public StatusToUpdate getStatusBeingProcessed(){
        return this.statusToUpdate;
    }

    public void processStatus(StatusToUpdate statusToUpdate){
        // TODO Implement process Logic
        log.info("Changed Status being processed from {} to {}", this.statusToUpdate, statusToUpdate );

        this.statusToUpdate = statusToUpdate;
    }

    public synchronized void processStatusToUpdate() {
        // TODO This will be the scheduled method
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

    public void resetStatusToUpdate(){
        statusToUpdate = null;
        // LAter also make the scheduling stop
    }
}
