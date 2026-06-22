package com.lostark.tracker.collect.error;

/**
 * 5xx or an I/O / read-timeout — transient. The retry layer (02-03) retries with exponential
 * backoff up to the attempt cap; if exhausted, only this item fails (D-10).
 */
public class TransientApiException extends LostarkApiException {

    public TransientApiException(String message) {
        super(message);
    }

    public TransientApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
