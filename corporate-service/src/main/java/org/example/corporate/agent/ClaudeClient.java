package org.example.corporate.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Thin wrapper over the Anthropic Messages API ({@code POST /v1/messages}).
 * Deliberately non-streaming: {@link AgentChatService} runs a multi-step tool
 * loop and streams its own SSE events to the browser, so each Claude call here
 * is a plain request/response. Keeping tool parsing out of a token stream makes
 * the loop far simpler.
 *
 * The request body is built by the caller (it owns the message list, tools, and
 * system prompt); this class only attaches auth headers and returns the parsed
 * JSON response.
 */
@Component
public class ClaudeClient {

    static final String ANTHROPIC_VERSION = "2023-06-01";

    private final WebClient webClient;

    public ClaudeClient(AgentProperties props, WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("x-api-key", props.apiKey())
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Sends a fully-formed Messages request body and returns the parsed response.
     *
     * @throws ClaudeApiException on non-2xx or transport failure — callers turn
     *         this into an SSE {@code error} event (headers already sent) or a
     *         502 before streaming starts.
     */
    public JsonNode createMessage(ObjectNode requestBody) {
        try {
            JsonNode response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(60));
            if (response == null) {
                throw new ClaudeApiException("Empty response from Claude API");
            }
            return response;
        } catch (WebClientResponseException e) {
            throw new ClaudeApiException(
                    "Claude API returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (ClaudeApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ClaudeApiException("Claude API call failed: " + e.getMessage(), e);
        }
    }
}
