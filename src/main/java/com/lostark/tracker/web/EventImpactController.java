package com.lostark.tracker.web;

import com.lostark.tracker.domain.EventType;
import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.read.EventImpactService;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.EnrichedEventImpactResponse;
import com.lostark.tracker.web.dto.EventImpactResponse;
import com.lostark.tracker.web.error.InvalidRequestException;
import com.lostark.tracker.web.error.ItemNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

/**
 * The event-impact read (IMPACT-01): {@code GET /api/items/{id}/event-impact?window=N} returns, per
 * game event, this item's pre/post anchor {@code min_price} and the {@code change_rate} across the
 * event's ±N-hour window (a temporal correlation, D-07). A separate controller from
 * {@link PricesController}; lives under {@code /api/items/**} which 04-02 already permitAll's.
 *
 * <p>{@code window} is a REQUIRED integer hours value. Validation mirrors {@code PricesController}:
 * the 400 window checks run BEFORE the 404 existence check (D-08/D-13), reusing
 * {@link InvalidRequestException} (400) and {@link ItemNotFoundException} (404) on the shared
 * {@code ApiExceptionHandler} contract — no new exception types. An omitted {@code window} ->
 * {@code MissingServletRequestParameterException} (400) and a non-integer -> {@code
 * MethodArgumentTypeMismatchException} (400) are already handled, never a 500.
 */
@RestController
@RequestMapping("/api/items")
public class EventImpactController {

    /** Staleness/N+1 aside, an unbounded window invites a huge range scan; 7 days caps it (D-08). */
    private static final int MAX_WINDOW_HOURS = 168;

    /**
     * Default page size when the client does not ask (2026-07-20). The endpoint used to return EVERY
     * event; at 10k events that is a 3~4MB response the client then renders in full.
     */
    private static final int DEFAULT_LIMIT = 50;
    /** Ceiling on {@code limit} — the same reasoning as MAX_WINDOW_HOURS: a bound the client cannot lift. */
    private static final int MAX_LIMIT = 200;

    /**
     * Frontend sort vocabulary -> {@code occurred_at} direction. A CLOSED set: anything else is a 400.
     *
     * <p>Silently falling back to the default would be worse than it looks — the UI would say "오래된순"
     * while the list stayed newest-first, i.e. "정렬했는데 안 바뀐다". This is the same rule
     * {@code MarketSearchService} applies to its sort vocabulary, for the same reason.
     */
    private static final Map<String, Sort.Direction> SORT = Map.of(
            "occurred_desc", Sort.Direction.DESC,
            "occurred_asc", Sort.Direction.ASC);

    private final EventImpactService eventImpactService;
    private final TrackedItemRepository trackedItemRepository;

    public EventImpactController(EventImpactService eventImpactService,
                                 TrackedItemRepository trackedItemRepository) {
        this.eventImpactService = eventImpactService;
        this.trackedItemRepository = trackedItemRepository;
    }

    /**
     * @param types comma-separated {@link EventType} names; omitted/blank means every type. An unknown
     *              name is a 400 rather than a silent drop — a typo would otherwise look like "that
     *              kind of event has no impact data" instead of "you asked for a type that does not exist".
     * @param sort  {@code occurred_desc} (default) or {@code occurred_asc}; see {@link #SORT}.
     * @param limit max events returned, 1..{@value #MAX_LIMIT}, default {@value #DEFAULT_LIMIT}.
     */
    @GetMapping("/{id}/event-impact")
    public EnrichedEventImpactResponse eventImpact(
            @PathVariable long id,
            @RequestParam int window,
            @RequestParam(required = false) String types,
            @RequestParam(defaultValue = "occurred_desc") String sort,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        // All parameter validation (400) BEFORE the existence check (404), per D-08/D-13.
        if (window <= 0) {
            throw new InvalidRequestException("window must be a positive number of hours");
        }
        if (window > MAX_WINDOW_HOURS) {
            throw new InvalidRequestException("window must be <= " + MAX_WINDOW_HOURS + " hours");
        }
        Sort.Direction direction = SORT.get(sort);
        if (direction == null) {
            throw new InvalidRequestException("sort must be one of: occurred_desc, occurred_asc");
        }
        if (limit < 1) {
            throw new InvalidRequestException("limit must be at least 1");
        }
        if (limit > MAX_LIMIT) {
            throw new InvalidRequestException("limit must be <= " + MAX_LIMIT);
        }
        Collection<EventType> eventTypes = parseTypes(types);
        // findById (same single read as existsById) also yields the item's enrichment for the wrapper.
        TrackedItem item = trackedItemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        // The controller wraps the service result to attach enrichment as top-level metadata (ITEM-04).
        EventImpactResponse base = eventImpactService.eventImpact(id, window, eventTypes, direction, limit);
        return new EnrichedEventImpactResponse(
                base.itemId(), base.window(), base.totalCount(),
                item.getIconUrl(), item.getItemGroup(), item.getRoleGroup(),
                base.events());
    }

    /**
     * {@code types} -> the event kinds to include. Omitted or blank means EVERY type, expressed as the
     * full enum set rather than null so the repository has a single query path (no nullable branch).
     *
     * <p>An unrecognized name is a 400 listing the valid values. Dropping it instead would turn a typo
     * into a plausible-looking empty result — the screen would say this item has no impact data for
     * that kind of event, which is a different claim from "no such kind exists".
     */
    private static Collection<EventType> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return EnumSet.allOf(EventType.class);
        }
        EnumSet<EventType> parsed = EnumSet.noneOf(EventType.class);
        for (String raw : types.split(",")) {
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                parsed.add(EventType.valueOf(name));
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException(
                        "unknown event type: " + name + " (valid: " + EnumSet.allOf(EventType.class) + ")");
            }
        }
        // "types=,," parses to nothing — treat it as no filter rather than "match none", which would
        // render an empty screen that looks like a data problem.
        return parsed.isEmpty() ? EnumSet.allOf(EventType.class) : parsed;
    }
}
