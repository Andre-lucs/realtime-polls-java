package com.andrelucs.realtimepolls.integrationtests.pollstatussystem;

import com.andrelucs.realtimepolls.data.PostgresNotificationListener;
import com.andrelucs.realtimepolls.integrationtests.AbstractIntegrationTest;
import com.andrelucs.realtimepolls.polls.PollRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("default")
class PostgresNotificationListenerTest extends AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PostgresNotificationListenerTest.class);
    @Autowired
    private DataSource dataSource;

    @Autowired
    private PostgresNotificationListener listener;

    @Autowired
    public PostgresNotificationListenerTest(PollRepository pollRepository) {
        super(pollRepository);
    }

    @Override
    protected void saveTestData() {
        // Don't need data
    }

    @AfterEach
    void ensureListenerIsActive(){
        listener.clearHandlers();
    }

    private void notifyPostgres(String channel, String payload) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("NOTIFY " + channel + ", '" + payload + "'");
            log.info("Sent notification '{}' to channel '{}'", payload, channel);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void shouldReceiveNotificationFromPostgres() throws Exception {

        String channel = "test_channel";
        AtomicReference<String> payload = new AtomicReference<>();

        listener.listen(channel, payload::set);

        notifyPostgres(channel, "hello_world");

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> "hello_world".equals(payload.get()));

        Assertions.assertEquals("hello_world", payload.get());
    }

    @Test
    void shouldNotifyMultipleHandlers() throws Exception {
        String channel = "multi_channel";

        AtomicInteger counter = new AtomicInteger(0);

        PostgresNotificationListener.PayloadHandler h1 = p -> counter.incrementAndGet();
        PostgresNotificationListener.PayloadHandler h2 = p -> counter.incrementAndGet();
        PostgresNotificationListener.PayloadHandler h3 = p -> counter.incrementAndGet();

        log.info("Listen 1");
        listener.listen(channel, h1);
        log.info("Listen 2");
        listener.listen(channel, h2);
        log.info("Listen 3");
        listener.listen(channel, h3);

        notifyPostgres(channel, "test");

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> counter.get() == 3);

        Assertions.assertEquals(3, counter.get());
    }

    @Test
    void shouldStopReceivingAfterUnlisten() throws Exception {
        String channel = "unlisten_channel";
        AtomicInteger counter = new AtomicInteger(0);

        PostgresNotificationListener.PayloadHandler handler = p -> counter.incrementAndGet();

        listener.listen(channel, handler);

        notifyPostgres(channel, "step1");

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> counter.get() == 1);

        listener.unlisten(channel, handler);

        notifyPostgres(channel, "step2");

        // pequeno delay para garantir que o listener teria consumido se estivesse ativo
        Thread.sleep(200);

        Assertions.assertEquals(1, counter.get());
    }

    @Test
    void shouldReconnectAndContinueReceivingNotifications() throws Exception {
        String channel = "reconnect_channel";
        AtomicReference<String> payload = new AtomicReference<>();

        listener.listen(channel, payload::set);

        // Forçar fechamento da conexão do listener
        log.warn("Forcing listener connection close...");
        listener.stop(); // interrompe thread

        // reinicia manualmente igual no lifecycle normal
        Thread.sleep(300);
        listener.start();

        // enviar notify depois de reiniciar
        notifyPostgres(channel, "after_reconnect");

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> "after_reconnect".equals(payload.get()));

        Assertions.assertEquals("after_reconnect", payload.get());
    }

    @Test
    void shouldRestoreListenSubscriptionsAfterConnectionLoss() throws Exception {
        String channel1 = "chan_a";
        String channel2 = "chan_b";

        AtomicReference<String> a = new AtomicReference<>();
        AtomicReference<String> b = new AtomicReference<>();

        listener.listen(channel1, a::set);
        listener.listen(channel2, b::set);

        // força reconexão
        listener.stop();
        listener.start();

        // testa ambas
        notifyPostgres(channel1, "A_msg");
        notifyPostgres(channel2, "B_msg");

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> "A_msg".equals(a.get()) && "B_msg".equals(b.get()));

        Assertions.assertEquals("A_msg", a.get());
        Assertions.assertEquals("B_msg", b.get());
    }

}
