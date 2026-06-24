package com.lostark.tracker.admin;

import com.lostark.tracker.domain.GameEvent;
import com.lostark.tracker.repository.GameEventRepository;
import com.lostark.tracker.web.dto.GameEventRequest;
import com.lostark.tracker.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Admin write/read service for game events (ADMIN-01). {@code create}/{@code replace} persist via the
 * entity, so {@code created_at}/{@code updated_at} are stamped by {@code @PrePersist}/{@code @PreUpdate}
 * (D-07) — never assigned here. {@code replace} is a FULL replace that allows {@code occurred_at} to
 * change (D-06). Missing ids on {@code replace}/{@code delete} raise {@link ResourceNotFoundException}
 * -> 404 (D-09). {@code list} sorts newest-occurrence-first (CONTEXT discretion).
 */
@Service
public class AdminEventService {

    private final GameEventRepository gameEventRepository;

    public AdminEventService(GameEventRepository gameEventRepository) {
        this.gameEventRepository = gameEventRepository;
    }

    public GameEvent create(GameEventRequest request) {
        return gameEventRepository.save(new GameEvent(
                request.eventType(), request.title(), request.occurredAt(), request.description()));
    }

    public List<GameEvent> list() {
        return gameEventRepository.findAll().stream()
                .sorted(Comparator.comparing(GameEvent::getOccurredAt).reversed())
                .toList();
    }

    public GameEvent replace(long id, GameEventRequest request) {
        GameEvent event = gameEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + id + " not found"));
        event.replace(request.eventType(), request.title(), request.occurredAt(), request.description());
        return gameEventRepository.save(event);
    }

    public void delete(long id) {
        if (!gameEventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event " + id + " not found");
        }
        gameEventRepository.deleteById(id);
    }
}