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
    private final Set<String> listeningChannels = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean isListening = new AtomicBoolean(false);


    @EventListener(ApplicationReadyEvent.class)
    public void start(){
        log.info("Starting postgres notification listener {}, {}" , Thread.currentThread().getName(), this);
        if (isListening.get()){
            stop();
        }
        initListenThread();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Listener {}, {}" , Thread.currentThread().getName(), this);
        isListening.set(false);
        closeConnection();

        if (listenThread != null){
            listenThread.interrupt();
            try { listenThread.join(); } catch (InterruptedException ignored) {}
            listenThread = null;
        }
    }

    public void clearHandlers() throws SQLException {
        channelHandlers.clear();
        ensureConnection();
        try (Statement stmt = sqlConnection.createStatement()) {
            stmt.execute("UNLISTEN *");
            listeningChannels.clear();
            log.info("Cleared all LISTEN subscriptions");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

        var handlers = channelHandlers.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet());
        handlers.add(handler);

        boolean shouldListen = listeningChannels.add(channel);
        if (shouldListen) {
            try (Statement st = sqlConnection.createStatement()) {
                st.execute("LISTEN " + channel);
                log.info("LISTEN issued for channel {}", channel);
            } catch (SQLException e) {
                // rollback the flag on failure
                listeningChannels.remove(channel);
            }
        }
    }

    public synchronized void unlisten(String channel, PayloadHandler handler) throws SQLException {
        var handlers = channelHandlers.get(channel);
        if (handlers == null) return;

        handlers.remove(handler);

        if (handlers.isEmpty()) {
            channelHandlers.remove(channel);

            // only UNLISTEN if we had previously LISTENed
            if (listeningChannels.remove(channel)) {
                try (Statement stmt = sqlConnection.createStatement()) {
                    stmt.execute("UNLISTEN " + channel);
                    log.info("UNLISTEN issued for channel {}", channel);
                }
            }
        }
    }

    public boolean isListening(String channel, PayloadHandler handler){
        return listeningChannels.contains(channel) && channelHandlers.containsKey(channel) && channelHandlers.get(channel).contains(handler);
    }

    private synchronized void restoreSubs(){
        try (Statement stmt = sqlConnection.createStatement()) {
            stmt.execute("UNLISTEN *"); // Ensures not getting remaining connections
            for (String channel : listeningChannels) {
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
            while (isListening.get() && !Thread.currentThread().isInterrupted()) {
                //log.info("Looped and waiting");
                ensureConnection();

                var notifications = pgConnection.getNotifications(postgresWaitInterval);
                if (notifications == null) continue;

                if (Thread.currentThread().isInterrupted()) break;

                for (PGNotification notification : notifications) {
                    String channel = notification.getName();
                    String payload = notification.getParameter();

                    var handlers = channelHandlers.get(channel);
                    if (handlers != null) {
                        log.info("Received notification from: {}, payload {} {}", channel, payload, Thread.currentThread());
                        handlers.forEach(h -> h.handle(payload));
                    }
                }

            }
        } catch (Exception e) {
            closeConnection();
            log.error("Notification listener error: {}", e.getMessage());
        } finally {
            isListening.set(false);
        }
    }

}
