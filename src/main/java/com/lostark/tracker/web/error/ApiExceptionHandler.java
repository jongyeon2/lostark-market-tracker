package com.lostark.tracker.web.error;

import com.lostark.tracker.collect.error.LostarkApiException;
import com.lostark.tracker.collect.error.RateLimitedApiException;
import com.lostark.tracker.web.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * The custom global error contract for the read API (D-10). Maps domain exceptions to the
 * consistent {@link ApiErrorResponse} JSON {@code {timestamp, status, error, message}} so every 4xx
 * response is uniform and trivial to assert against.
 *
 * <ul>
 *   <li>404 — {@link ItemNotFoundException} (missing item / no price yet) [03-01].</li>
 *   <li>400 — {@link InvalidRequestException} (bad time window) and malformed/missing request
 *       parameters (unparseable {@code from}/{@code to}) [03-03] (D-13).</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleItemNotFound(ItemNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Generalized admin-layer 404 (missing event/item on PUT/DELETE) — the D-09 contract. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Admin create of an already-active unique resource (duplicate external_item_id) -> 409 (D-05). */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * {@code @Valid} body violations on the admin write surface — blank title, null event_type /
     * occurred_at, blank externalItemId / displayName — collapse to a 400 on the shared contract
     * (D-10). The message joins the offending fields ("field: reason"); it never echoes a secret.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** A {@code from}/{@code to} that cannot be parsed as a date — a 400, never a 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'");
    }

    /**
     * The market-search read path (아바타·모험의 서) hits 로스트아크 on-demand, so its upstream failures reach
     * the controller (the collector, by contrast, absorbs them internally). A shared-bucket throttle or a real
     * 429 → 429 with the caller's own wording; any other upstream fault (auth/5xx/I-O/4xx) → 502, since it is
     * the upstream we could not reach, not the client's request. The message never carries a key/secret (D-08).
     */
    @ExceptionHandler(RateLimitedApiException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimited(RateLimitedApiException ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(LostarkApiException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstream(LostarkApiException ex) {
        return error(HttpStatus.BAD_GATEWAY, "로스트아크 API에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message);
        return ResponseEntity.status(status).body(body);
    }
}
