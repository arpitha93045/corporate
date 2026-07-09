package com.corporate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.corporate.mail.MailService;

/**
 * Configuration for the AI Gifting Agent, mirroring the @Value style used by
 * MailService. Gated on {@link #enabled()} + a non-blank {@link #apiKey()} so
 * the endpoint stays inert (503) until both are supplied — no live key is
 * needed to compile or boot.
 */
@Component
public class AgentProperties {

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int maxTokens;
    private final int maxToolIterations;

    public AgentProperties(
            @Value("${app.agent.enabled:false}") boolean enabled,
            @Value("${app.agent.api-key:}") String apiKey,
            @Value("${app.agent.model:claude-sonnet-4-6}") String model,
            @Value("${app.agent.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${app.agent.max-tokens:1024}") int maxTokens,
            @Value("${app.agent.max-tool-iterations:6}") int maxToolIterations
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.maxTokens = maxTokens;
        this.maxToolIterations = maxToolIterations;
    }

    /** True only when the feature is switched on AND an API key is configured. */
    public boolean isUsable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public boolean enabled() { return enabled; }
    public String apiKey() { return apiKey; }
    public String model() { return model; }
    public String baseUrl() { return baseUrl; }
    public int maxTokens() { return maxTokens; }
    public int maxToolIterations() { return maxToolIterations; }
}
