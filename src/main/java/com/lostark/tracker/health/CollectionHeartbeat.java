package com.lostark.tracker.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * Reports each collection tick's outcome to an external dead-man switch (healthchecks.io) so a dead
 * scheduler or a total-failure tick surfaces on Discord — the silence that {@code /api/health/collection}
 * cannot catch on its own, because it just echoes the last {@code collection_run} row (design spec §3).
 *
 * <p><b>Ping rule (§4):</b> the target is chosen by the {@code succeeded} count, NOT the status string.
 * {@code succeeded > 0} pings the base URL ("pipeline alive"); {@code succeeded == 0} pings {@code /fail}
 * for an immediate alert. status is avoided because an empty watchlist records a false {@code SUCCESS}
 * ({@code 0 == items.size()}) — a bug the repo already documents (application-prod.yml:6-8); the
 * count-based rule is immune to that timing race.
 *
 * <p><b>Fail-open (§7):</b> this rides on top of collection and must never break it. A blank ping-url is
 * a silent no-op (dev/test/CI stay dark), and any ping failure is swallowed.
 *
 * <p><b>Secret discipline (§5):</b> the ping URL is a secret — whoever holds it can forge "alive" and
 * silence the alarm forever. Spring's {@code ResourceAccessException} embeds the request URL in its
 * message ({@code I/O error on GET request for "<url>": ...}), so the exception object is NEVER handed to
 * the logger. Only the exception's class name is logged; no message, no stack trace. Locked by
 * {@code CollectionHeartbeatTest#pingUrl_neverAppearsInLogs}. This mirrors the shell rule the repo
 * already keeps in {@code backup-db.sh:78}.
 *
 * <p>The per-call timeout ({@code SimpleClientHttpRequestFactory}, ~3s) is configured on the injected
 * builder by {@link MonitoringConfig} — the same split as {@link com.lostark.tracker.collect.ApiClientConfig}
 * / {@code LostarkApiClient}, which keeps this unit-testable with a mock-bound builder.
 */
public class CollectionHeartbeat {

    private static final Logger log = LoggerFactory.getLogger(CollectionHeartbeat.class);

    /** Normalized base ping URL, or "" when monitoring is disabled. */
    private final String pingUrl;
    private final RestClient restClient;

    public CollectionHeartbeat(RestClient.Builder builder, String pingUrl) {
        this.pingUrl = normalize(pingUrl);
        // No baseUrl and no requestFactory set here: report() uses an absolute URI, and the timeout
        // factory lives on the injected builder (MonitoringConfig) so tests can bind a mock server.
        this.restClient = builder.build();
    }

    /**
     * Ping the dead-man switch for the tick that just finished. No-op when disabled (blank URL).
     * Fail-open: never throws — a ping problem cannot affect the collection tick that called it.
     *
     * @param succeeded number of items whose snapshot was persisted this tick
     */
    public void report(int succeeded) {
        if (pingUrl.isEmpty()) {
            return;
        }
        String target = succeeded > 0 ? pingUrl : pingUrl + "/fail";
        try {
            restClient.get().uri(URI.create(target)).retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            // §5: log the class name ONLY. Passing `e` (or its message/stack trace) would leak the
            // ping URL — ResourceAccessException carries it in the first line of the message.
            log.warn("collection heartbeat ping failed ({})", e.getClass().getSimpleName());
        }
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
