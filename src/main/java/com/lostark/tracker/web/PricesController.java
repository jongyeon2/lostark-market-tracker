package com.lostark.tracker.web;

import com.lostark.tracker.read.DownsampleService;
import com.lostark.tracker.read.DownsampleService.DownsampleResult;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.read.WindowQueryService;
import com.lostark.tracker.read.WindowQueryService.WindowResult;
import com.lostark.tracker.repository.ItemDailyStatRepository;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.DailyStatPoint;
import com.lostark.tracker.web.dto.EventPoint;
import com.lostark.tracker.web.dto.TimelineResponse;
import com.lostark.tracker.web.error.InvalidRequestException;
import com.lostark.tracker.web.error.ItemNotFoundException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * The timeline read (API-03/API-04): {@code GET /api/items/{id}/prices?from=&to=} returns the
 * window's snapshots and overlapping events as two distinct arrays (D-04), auto-downsampling the
 * snapshots server-side for large ranges (D-08). A separate controller from {@link ItemController}.
 *
 * <p>{@code from}/{@code to} are parsed as UTC instants (no KST shift, D-11). The window read is
 * delegated to the shared {@link WindowQueryService} (4A) and the snapshots pass through
 * {@link DownsampleService} before response assembly; events are never downsampled. Range results
 * are not cached (only latest is cached).
 */
@RestController
@RequestMapping("/api/items")
public class PricesController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TrackedItemRepository trackedItemRepository;
    private final WindowQueryService windowQueryService;
    private final DownsampleService downsampleService;
    private final ItemDailyStatRepository itemDailyStatRepository;

    public PricesController(TrackedItemRepository trackedItemRepository,
                            WindowQueryService windowQueryService,
                            DownsampleService downsampleService,
                            ItemDailyStatRepository itemDailyStatRepository) {
        this.trackedItemRepository = trackedItemRepository;
        this.windowQueryService = windowQueryService;
        this.downsampleService = downsampleService;
        this.itemDailyStatRepository = itemDailyStatRepository;
    }

    @GetMapping("/{id}/prices")
    public TimelineResponse prices(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        // Range validation (400) BEFORE the existence check (404), per D-13: to <= from covers both
        // from>to and a zero/negative window. A valid window with no rows stays a 200 empty below.
        if (!to.isAfter(from)) {
            throw new InvalidRequestException("from must be before to (window must be positive)");
        }
        // Missing item -> 404 via the shared 03-01 contract; "valid window, no data" stays a 200 below.
        // findById (same single read as existsById) also yields the item's enrichment for the response.
        TrackedItem item = trackedItemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        WindowResult window = windowQueryService.fetchWindow(id, from, to);
        DownsampleResult downsampled = downsampleService.downsample(id, from, to, window.snapshots());
        List<EventPoint> events = window.events().stream().map(EventPoint::from).toList();

        // Additive backfill merge (BACKFILL-03): the window's daily-average rows as a SEPARATE array
        // from the real-time snapshots (D-02 no-mix). The KST calendar-day window mirrors how the
        // backfill sources stamp stat_date; empty when there is none (200 preserved). The price_snapshot
        // read path (WindowQueryService/DownsampleService) is untouched.
        LocalDate fromDate = from.atZoneSameInstant(KST).toLocalDate();
        LocalDate toDate = to.atZoneSameInstant(KST).toLocalDate();
        List<DailyStatPoint> backfill = itemDailyStatRepository
                .findByTrackedItemIdAndStatDateBetweenOrderByStatDateAsc(id, fromDate, toDate)
                .stream().map(DailyStatPoint::from).toList();

        return new TimelineResponse(
                downsampled.downsampled(),
                downsampled.bucketWidth(),
                downsampled.snapshots(),
                events,
                item.getIconUrl(),
                item.getItemGroup(),
                item.getRoleGroup(),
                backfill);
    }
}
