package com.lostark.tracker.gem;

import com.lostark.tracker.gem.GemDtos.GemPrice;
import com.lostark.tracker.gem.GemDtos.GemPriceStatus;
import com.lostark.tracker.ratelimit.RedisTokenBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The ONE place a gem price is fetched from 경매장 (Phase 27, extracted from {@code GemService}).
 *
 * <p><b>Why this class exists.</b> Two callers now need a gem price: {@link GemService} (serving
 * {@code /api/gems}) and {@link GemPriceRecorder} (the hourly record). The Phase 26 live bug was gems
 * BYPASSING the project's shared {@link RedisTokenBucket} — 경매장 and 거래소 share one server-side
 * per-key quota (Phase 24 §H2), so an unmetered gem call raced the 10분 수집 틱 into a real 429. If each
 * caller acquired its own token, there would be two places to make that mistake again. There is one.
 *
 * <p>Everything below is behaviour moved verbatim from {@code GemService} — the per-row isolation, the
 * status mapping, and the fail-closed token check are unchanged.
 */
@Component
class GemPriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(GemPriceFetcher.class);

    private final LostarkAuctionClient client;
    private final RedisTokenBucket rateLimiter;

    GemPriceFetcher(LostarkAuctionClient client, RedisTokenBucket rateLimiter) {
        this.client = client;
        this.rateLimiter = rateLimiter;
    }

    /**
     * One call per gem — 경매장 offers no batch lookup, and each gem is isolated by an exact ItemName
     * (Phase 24: the level exists only in the name, there is no numeric level filter).
     *
     * <p>A single gem's failure is swallowed into a {@code FETCH_FAILED} row so it can never blank the
     * other five — the same per-row isolation {@code ItemCard} applies to a 404 latest-price.
     */
    List<GemPrice> fetchAll() {
        List<GemPrice> gems = new ArrayList<>(GemCatalog.ENTRIES.size());
        for (GemCatalog.Entry entry : GemCatalog.ENTRIES) {
            gems.add(fetchOne(entry));
        }
        return gems;
    }

    private GemPrice fetchOne(GemCatalog.Entry entry) {
        // The SAME global bucket the collector spends from — "one API key, one bucket" (D-03). 경매장 and
        // 거래소 share one server-side per-key quota (Phase 24 §H2), so a gem call that skipped this
        // limiter would spend budget the app never accounted for and race the 10분 틱 into a real 429 —
        // which is exactly what happened on the first live run (겁화 3 OK, 작열 3 × TooManyRequests).
        // Yielding here keeps the collector whole: Core Value outranks both gem callers.
        if (!rateLimiter.tryAcquire()) {
            return row(entry, null, GemPriceStatus.RATE_LIMITED);
        }
        try {
            Optional<Long> lowest = client.findLowestBuyPrice(entry.searchName());
            // empty = 매물은 있으나 즉시구매를 건 것이 없음(입찰 전용) → 값을 지어내지 않고 상태로 말한다.
            return lowest
                    .map(price -> row(entry, price, GemPriceStatus.OK))
                    .orElseGet(() -> row(entry, null, GemPriceStatus.NO_BUYOUT));
        } catch (HttpClientErrorException.TooManyRequests e) {
            // The bucket granted a token but the server refused anyway — the app's 90-token bucket can
            // legitimately outrun the real 100/min window (a full bucket plus a minute of refill is up to
            // 180 calls), and dev startup fires the 49-item tick and the backfill runner at once. This is
            // pre-existing shared-limiter behaviour (Phase 2), NOT something gems can fix without editing
            // Core Value code — the collector already absorbs 429 via Retry-After. Gems just tell the truth:
            // same meaning as a local throttle (설계된 양보, 곧 회복), so same status.
            log.warn("gem price throttled by API (429) for level {} {} — row marked RATE_LIMITED",
                    entry.level(), entry.series());
            return row(entry, null, GemPriceStatus.RATE_LIMITED);
        } catch (Exception e) {
            // Log the class only — never the key/secret (NewsService precedent).
            log.warn("gem price fetch failed for level {} {} ({}) — row marked FETCH_FAILED",
                    entry.level(), entry.series(), e.getClass().getSimpleName());
            return row(entry, null, GemPriceStatus.FETCH_FAILED);
        }
    }

    private static GemPrice row(GemCatalog.Entry entry, Long price, GemPriceStatus status) {
        return new GemPrice(entry.series(), entry.level(), entry.displayName(), entry.iconUrl(), price, status);
    }
}