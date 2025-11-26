package com.andrelucs.realtimepolls.data;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class PostgresNotificationListener {

    public interface PayloadHandler{
        void handle(String payload);
    }
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    @Value("${spring.datasource.username}")
    private String jdbcUser;
    @Value("${spring.datasource.password}")
    private String jdbcPass;

    private volatile PGConnection pgConnection;
    private volatile Connection sqlConnection;
    private Thread listenThread;

    @Value("${pg-listener.wait-ms:500}")
    private int postgresWaitInterval;

    private final Map<String, Set<PayloadHandler>> channelHandlers = new ConcurrentHashMap<>();
    private final AtomicBoolean isListening = new AtomicBoolean(false);


    @EventListener(ApplicationReadyEvent.class)
    public void start(){
        log.info("The postgresWaitInterval is {}", postgresWaitInterval);
        if (isListening.get()){
            stop();
        }
        initListenThread();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Listener");
        isListening.set(false);
        if (listenThread != null){
            try { listenThread.join(postgresWaitInterval); } catch (InterruptedException ignored) {}
            if(listenThread.isAlive()) listenThread.interrupt();
            listenThread = null;
        }

        closeConnection();
    }

    public void clearHandlers(){
        channelHandlers.clear();
    }

    private void ensureConnection() throws SQLException {
        if (sqlConnection == null || sqlConnection.isClosed()) {
            closeConnection();
            sqlConnection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPass);
            pgConnection = sqlConnection.unwrap(PGConnection.class);

            restoreSubs();
        }
    }

    private void closeConnection(){
        try { if (sqlConnection != null) sqlConnection.close(); }
        catch (Exception ignored) {}
        sqlConnection = null;
        pgConnection = null;
    }

    private void initListenThread() {
        try {
            ensureConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        listenThread = Thread.ofVirtual()
                .name("pg-notify-listener")
                .start(this::listenLoop);
    }

    public synchronized void listen(String channel, PayloadHandler handler) throws SQLException {
        ensureConnection();

        boolean firstHandler = channelHandlers.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).isEmpty();
        channelHandlers.get(channel).add(handler);

        if (firstHandler) {
            try (Statement st = sqlConnection.createStatement()) {
                st.execute("LISTEN " + channel);
            }
        }

    }

    public synchronized void unlisten(String channel, PayloadHandler handler) throws SQLException {
        var handlers = channelHandlers.get(channel);
        if (handlers == null) return;

        handlers.remove(handler);

        // Se não tiver mais handlers → remover LISTEN
        if (handlers.isEmpty()) {
            try (Statement stmt = sqlConnection.createStatement()) {
                stmt.execute("UNLISTEN " + channel);
            }
            log.info("Removed LISTEN from {}", channel);
        }
    }

    private synchronized void restoreSubs(){
        try (Statement stmt = sqlConnection.createStatement()) {
            for (String channel : channelHandlers.keySet()) {
                stmt.execute("LISTEN " + channel);
                log.info("Restored LISTEN for channel: {}", channel);
            }
        } catch (SQLException e) {
            log.error("Failed restoring LISTEN channels: {}", e.getMessage());
        }
    }

    private void listenLoop() {
        log.info("Notification thread initiated.");
        isListening.set(true);
        try {
            while (isListening.get()) {
                //log.info("Looped and waiting");
                ensureConnection();

                var notifications = pgConnection.getNotifications(postgresWaitInterval);

                if (notifications == null) continue;

                for (PGNotification notification : notifications) {
                    String channel = notification.getName();
                    String payload = notification.getParameter();

                    var handlers = channelHandlers.get(channel);
                    if (handlers != null) {
                        log.info("Received notification from: {}", channel);
                        handlers.forEach(h -> h.handle(payload));
                    }
                }

            }
        } catch (Exception e) {
            log.error("Notification listener error: {}", e.getMessage());
        }
        isListening.set(false);
    }

}
