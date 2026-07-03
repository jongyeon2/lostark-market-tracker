package com.lostark.tracker.admin;

import com.lostark.tracker.domain.TrackedItem;
import com.lostark.tracker.repository.TrackedItemRepository;
import com.lostark.tracker.web.dto.TrackedItemRequest;
import com.lostark.tracker.web.error.DuplicateResourceException;
import com.lostark.tracker.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Admin write service for watchlist items (ADMIN-02). {@code create} branches on
 * {@code findByExternalItemId} (D-05): active duplicate -> 409, soft-deleted -> reactivate (200),
 * absent -> fresh insert (201). {@code delete} is a SOFT delete ({@code active=false}, D-03) that is
 * idempotent and preserves the row + its {@code price_snapshot} history (the FK has no ON DELETE
 * CASCADE); a missing id raises {@link ResourceNotFoundException} -> 404 (D-09).
 */
@Service
public class AdminItemService {

    private final TrackedItemRepository trackedItemRepository;

    public AdminItemService(TrackedItemRepository trackedItemRepository) {
        this.trackedItemRepository = trackedItemRepository;
    }

    /** Outcome of an item create: the persisted item plus whether it was a fresh insert (201) or a reactivation (200). */
    public record UpsertResult(TrackedItem item, boolean created) {
    }

    /**
     * Read-only source of truth for the admin console watchlist (ADMINUI-04, D-13): EVERY tracked item,
     * active AND soft-deleted (inactive), unlike the public {@code findByActiveTrue} (active-only, D-12).
     * Returned in a deterministic order — active items first, then by {@code displayName} ascending — so
     * the console renders a stable list across refreshes rather than JPA's incidental insertion order.
     * Reuses the inherited {@code findAll()} + an in-memory sort: no write, no new query, no cache/
     * collection/event-impact touch (Core Value guard, D-14).
     */
    public List<TrackedItem> listAll() {
        return trackedItemRepository.findAll().stream()
                .sorted(Comparator.comparing(TrackedItem::isActive).reversed()
                        .thenComparing(TrackedItem::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public UpsertResult create(TrackedItemRequest request) {
        return trackedItemRepository.findByExternalItemId(request.externalItemId())
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new DuplicateResourceException(
                                "Item with externalItemId " + request.externalItemId() + " already exists");
                    }
                    // Soft-deleted -> reactivate and refresh the human-facing fields (D-05).
                    existing.setActive(true);
                    existing.setDisplayName(request.displayName());
                    existing.setCategory(request.category());
                    return new UpsertResult(trackedItemRepository.save(existing), false);
                })
                .orElseGet(() -> {
                    TrackedItem saved = trackedItemRepository.save(new TrackedItem(
                            request.externalItemId(), request.displayName(), request.category()));
                    return new UpsertResult(saved, true);
                });
    }

    public void delete(long id) {
        TrackedItem item = trackedItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item " + id + " not found"));
        if (item.isActive()) {
            // Soft delete: drop it from findByActiveTrue + GET /api/items while keeping the row and
            // all price_snapshot history. Idempotent — an already-inactive item is a no-op (D-03).
            item.setActive(false);
            trackedItemRepository.save(item);
        }
    }
}