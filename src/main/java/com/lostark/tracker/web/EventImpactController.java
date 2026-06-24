package com.lostark.tracker.web;

import com.lostark.tracker.read.EventImpactService;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.EventImpactResponse;
import com.lostark.tracker.web.error.InvalidRequestException;
import com.lostark.tracker.web.error.ItemNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    private final EventImpactService eventImpactService;
    private final TrackedItemRepository trackedItemRepository;

    public EventImpactController(EventImpactService eventImpactService,
                                 TrackedItemRepository trackedItemRepository) {
        this.eventImpactService = eventImpactService;
        this.trackedItemRepository = trackedItemRepository;
    }

    @GetMapping("/{id}/event-impact")
    public EventImpactResponse eventImpact(@PathVariable long id, @RequestParam int window) {
        // Window validation (400) BEFORE the existence check (404), per D-08/D-13.
        if (window <= 0) {
            throw new InvalidRequestException("window must be a positive number of hours");
        }
        if (window > MAX_WINDOW_HOURS) {
            throw new InvalidRequestException("window must be <= " + MAX_WINDOW_HOURS + " hours");
        }
        if (!trackedItemRepository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        return eventImpactService.eventImpact(id, window);
    }
}
