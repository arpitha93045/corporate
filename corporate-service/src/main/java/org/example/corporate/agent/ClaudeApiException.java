package org.example.corporate.agent;

/** Raised when a Claude Messages API call fails (non-2xx or transport error). */
public class ClaudeApiException extends RuntimeException {
    public ClaudeApiException(String message) {
        super(message);
    }

    public ClaudeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
