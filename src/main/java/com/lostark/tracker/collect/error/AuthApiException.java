package com.lostark.tracker.collect.error;

/**
 * 401/403 — fatal auth (bad/expired key). The caller MUST NOT retry; it stops new outbound
 * calls and converges the tick to failure (D-08). Never include the key in the message.
 */
public class AuthApiException extends LostarkApiException {

    public AuthApiException(String message) {
        super(message);
    }
}
