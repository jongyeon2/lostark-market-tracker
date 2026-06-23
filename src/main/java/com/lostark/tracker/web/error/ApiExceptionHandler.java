package com.lostark.tracker.web.error;

import com.lostark.tracker.web.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * The custom global error contract for the read API (D-10). Maps domain exceptions to the
 * consistent {@link ApiErrorResponse} JSON {@code {timestamp, status, error, message}} so 4xx
 * responses are uniform and trivial to assert against.
 *
 * <p>This plan lays the 404 half ({@link ItemNotFoundException}); the 400 input-validation
 * handlers (from&gt;to / window&le;0 / bad params) are added in 03-03 against this same advice.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleItemNotFound(ItemNotFoundException ex) {
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
