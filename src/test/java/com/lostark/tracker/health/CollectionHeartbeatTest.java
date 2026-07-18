package com.lostark.tracker.health;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Locks the {@link CollectionHeartbeat} contract (design spec §4/§5/§7/§9): blank ping-url is a no-op,
 * the ping target is chosen by the {@code succeeded} count (not a status string), a failing ping is
 * swallowed (fail-open — it must never break the collection tick), and — the security regression —
 * the secret ping URL never reaches the logs. Pure MockRestServiceServer, no Docker/Spring context.
 *
 * <p>The URL below is a fabricated placeholder, not a real ping secret.
 */
class CollectionHeartbeatTest {

    private static final String BASE = "https://hc-ping.test/00000000-0000-0000-0000-000000000000";

    private record Fixture(CollectionHeartbeat heartbeat, MockRestServiceServer server) {
    }

    /** Mirrors LostarkApiClientTest: bind the mock to a plain builder the component will build from. */
    private Fixture fixture(String pingUrl) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new CollectionHeartbeat(builder, pingUrl), server);
    }

    private ch.qos.logback.classic.Logger capturedLogger;
    private ListAppender<ILoggingEvent> capturedAppender;

    private ListAppender<ILoggingEvent> attachLogCaptor() {
        capturedLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CollectionHeartbeat.class);
        capturedAppender = new ListAppender<>();
        capturedAppender.start();
        capturedLogger.addAppender(capturedAppender);
        return capturedAppender;
    }

    @AfterEach
    void detachLogCaptor() {
        if (capturedLogger != null) {
            capturedLogger.detachAppender(capturedAppender);
            capturedLogger = null;
            capturedAppender = null;
        }
    }

    @Test
    void blankPingUrl_makesNoHttpCall() {
        Fixture f = fixture("");   // disabled
        // No expectations registered: any outbound request would fail verification.
        f.heartbeat().report(3);
        f.server().verify();       // passes only because zero requests were made
    }

    @Test
    void succeededPositive_pingsBaseUrl() {
        Fixture f = fixture(BASE);
        f.server().expect(requestTo(BASE)).andExpect(method(GET)).andRespond(withSuccess());

        f.heartbeat().report(2);

        f.server().verify();
    }

    @Test
    void succeededZero_pingsFailEndpoint() {
        Fixture f = fixture(BASE);
        f.server().expect(requestTo(BASE + "/fail")).andExpect(method(GET)).andRespond(withSuccess());

        f.heartbeat().report(0);

        f.server().verify();
    }

    @Test
    void pingThatThrows_isSwallowed() {
        Fixture f = fixture(BASE);
        f.server().expect(requestTo(BASE)).andRespond(withServerError());

        // fail-open: a broken heartbeat must never propagate into the collection tick.
        assertThatCode(() -> f.heartbeat().report(1)).doesNotThrowAnyException();

        f.server().verify();
    }

    @Test
    void pingUrl_neverAppearsInLogs() {
        Fixture f = fixture(BASE);
        // I/O failure → RestClient wraps it in a ResourceAccessException whose message embeds the
        // request URL ("I/O error on GET request for \"<url>\": ..."). This is exactly the §5 threat.
        f.server().expect(requestTo(BASE)).andRespond(withException(new IOException("Connection timed out")));
        ListAppender<ILoggingEvent> logs = attachLogCaptor();

        f.heartbeat().report(1);

        assertThat(logs.list).isNotEmpty();               // it did log the failure...
        for (ILoggingEvent event : logs.list) {
            assertThat(event.getFormattedMessage()).doesNotContain(BASE);
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNull();   // no stack trace (first line carries the URL)
        }
    }
}
