package org.example.corporate.agent;

/**
 * Raised when the agent endpoint is hit while the feature is switched off or no
 * API key is configured. Surfaces as a 503 via GlobalExceptionHandler.
 */
public class AgentDisabledException extends RuntimeException {
    public AgentDisabledException(String message) {
        super(message);
    }
}
