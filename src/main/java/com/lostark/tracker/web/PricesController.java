package com.lostark.tracker.web;

import com.lostark.tracker.read.WindowQueryService;
import com.lostark.tracker.read.WindowQueryService.WindowResult;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.EventPoint;
import com.lostark.tracker.web.dto.SnapshotPoint;
import com.lostark.tracker.web.dto.TimelineResponse;
import com.lostark.tracker.web.error.ItemNotFoundException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The timeline read (API-03): {@code GET /api/items/{id}/prices?from=&to=} returns the window's
 * snapshots and the overlapping events as two distinct arrays (D-04). A separate controller from
 * {@link ItemController} to keep file ownership disjoint across 03-01/03-02.
 *
 * <p>{@code from}/{@code to} are parsed as UTC instants (no server-side KST conversion, D-11) and the
 * window read is delegated to the shared {@link WindowQueryService} (4A). Range validation
 * ({@code from>to}, {@code window<=0} -> 400) and server-side downsampling are added in 03-03 — this
 * endpoint returns RAW snapshots. Range results are not cached (only latest is cached).
 */
@RestController
@RequestMapping("/api/items")
public class PricesController {

    private final TrackedItemRepository trackedItemRepository;
    private final WindowQueryService windowQueryService;

    public PricesController(TrackedItemRepository trackedItemRepository,
                            WindowQueryService windowQueryService) {
        this.trackedItemRepository = trackedItemRepository;
        this.windowQueryService = windowQueryService;
    }

    @GetMapping("/{id}/prices")
    public TimelineResponse prices(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        // Missing item -> 404 via the shared 03-01 contract; "valid window, no data" stays a 200 below.
        if (!trackedItemRepository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }

        WindowResult window = windowQueryService.fetchWindow(id, from, to);
        List<SnapshotPoint> snapshots = window.snapshots().stream().map(SnapshotPoint::from).toList();
        List<EventPoint> events = window.events().stream().map(EventPoint::from).toList();
        return new TimelineResponse(snapshots, events);
    }
}
