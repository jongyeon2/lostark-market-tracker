package com.lostark.tracker.web.admin;

import com.lostark.tracker.admin.AdminEventService;
import com.lostark.tracker.web.dto.GameEventRequest;
import com.lostark.tracker.web.dto.GameEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin CRUD for game events behind the (04-02) shared-secret gate (ADMIN-01): POST 201 + created
 * resource, GET 200 (occurred_at desc), PUT 200 full replace, DELETE 204 empty body (D-06, D-08).
 * Binds {@link GameEventRequest} (never the entity) to block mass-assignment of id/timestamps
 * (D-09, D-10). Never logs request bodies or any header.
 */
@RestController
@RequestMapping("/api/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @PostMapping
    public ResponseEntity<GameEventResponse> create(@Valid @RequestBody GameEventRequest request) {
        GameEventResponse body = GameEventResponse.from(adminEventService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<GameEventResponse> list() {
        return adminEventService.list().stream().map(GameEventResponse::from).toList();
    }

    @PutMapping("/{id}")
    public GameEventResponse replace(@PathVariable long id, @Valid @RequestBody GameEventRequest request) {
        return GameEventResponse.from(adminEventService.replace(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        adminEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}