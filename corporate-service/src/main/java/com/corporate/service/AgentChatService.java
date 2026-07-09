package com.corporate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import com.corporate.client.ClaudeApiException;
import com.corporate.client.ClaudeClient;
import com.corporate.config.AgentProperties;
import com.corporate.config.AgentToolDefinitions;
import com.corporate.dto.AgentChatRequest;
import com.corporate.dto.AgentProductRef;
import com.corporate.dto.DraftCartDto;
import com.corporate.web.AgentDisabledException;

/**
 * Orchestrates one agent chat turn: builds the Claude request (system prompt +
 * cached catalog snapshot + client history), runs a bounded tool loop, and
 * streams progress to the browser as SSE events.
 *
 * SSE event names emitted:
 *   - tool       : {"name": "...", "input": {...}}   one per tool call
 *   - draft_cart : DraftCartDto                       emitted when create_draft_cart runs, so the
 *                                                     browser can adopt the priced proposal by token
 *   - message    : {"text": "..."}                    final assistant prose
 *   - done       : {}                                 terminal, closes the stream
 *   - error      : {"message": "..."}                 recoverable failure mid-stream
 *
 * Guardrails (PLAN §11.2 / §11.5): the model never prices — only estimate_total
 * does; per-line/quantity caps live in AgentTools; the iteration cap here stops a
 * runaway tool loop. Every tool call is logged for audit.
 */
@Service
public class AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatService.class);

    private static final String SYSTEM_INSTRUCTIONS = """
            You are the gifting concierge for a B2B corporate gifting store. Buyers describe an \
            occasion, audience, budget, and any dietary or cultural constraints; you propose a short, \
            curated selection (3-5 products) with brief reasoning, sensible quantities, and a total.

            Rules:
            - Use search_products and get_product to ground every suggestion in the real catalog. \
            Never invent products, slugs, or prices.
            - Always call estimate_total for any price or total. Never do money arithmetic yourself. \
            Prices are in paise (100 paise = 1 rupee).
            - Ask at most one or two clarifying questions if the request is ambiguous; otherwise propose.
            - Respect stated budgets and constraints (e.g. vegetarian -> filter dietary:vegetarian).
            - You propose only; the buyer completes checkout themselves. Do not promise to place orders.
            - Keep prose tight. When you present a final selection, list each product with quantity and \
            why it fits, then the estimated total.""";

    private final ClaudeClient claude;
    private final AgentToolDefinitions toolDefs;
    private final AgentTools tools;
    private final AgentProperties props;
    private final ObjectMapper mapper;
    private final AgentMetrics metrics;

    public AgentChatService(ClaudeClient claude,
                            AgentToolDefinitions toolDefs,
                            AgentTools tools,
                            AgentProperties props,
                            ObjectMapper mapper,
                            AgentMetrics metrics) {
        this.claude = claude;
        this.toolDefs = toolDefs;
        this.tools = tools;
        this.props = props;
        this.mapper = mapper;
        this.metrics = metrics;
    }

    /** Entry point. Throws AgentDisabledException before any streaming if unusable. */
    public void stream(AgentChatRequest request, SseEmitter emitter) {
        if (!props.isUsable()) {
            throw new AgentDisabledException("AI gifting assistant is not enabled");
        }
        try {
            runLoop(request, emitter);
            emitter.complete();
        } catch (ClaudeApiException e) {
            log.warn("agent.error upstream={}", e.getMessage());
            emitError(emitter, "The AI assistant is temporarily unavailable.");
            emitter.complete();
        } catch (Exception e) {
            log.error("agent.error unexpected", e);
            emitError(emitter, "Something went wrong while handling your request.");
            emitter.complete();
        }
    }

    private void runLoop(AgentChatRequest request, SseEmitter emitter) throws IOException {
        ArrayNode messages = buildInitialMessages(request);
        ArrayNode toolSchemas = toolDefs.toolSchemas();
        AgentMetrics.ChatRecorder rec = metrics.startChat();

        try {
            for (int iteration = 0; iteration < props.maxToolIterations(); iteration++) {
                ObjectNode body = buildRequestBody(messages, toolSchemas);
                JsonNode response = claude.createMessage(body);

                JsonNode usage = response.path("usage");
                rec.addTokens(usage.path("input_tokens").asLong(0), usage.path("output_tokens").asLong(0));

                String stopReason = response.path("stop_reason").asText("");
                JsonNode content = response.path("content");

                // Echo any assistant text in this turn to the client.
                emitAssistantText(emitter, content);

                if (!"tool_use".equals(stopReason)) {
                    // end_turn, max_tokens, stop_sequence, etc. — conversation turn done.
                    sendEvent(emitter, "done", mapper.createObjectNode());
                    return;
                }

                // The assistant asked to use one or more tools. Append its turn verbatim,
                // then run each tool and append the results as a user turn.
                messages.add(assistantTurn(content));
                ObjectNode toolResultTurn = mapper.createObjectNode();
                toolResultTurn.put("role", "user");
                ArrayNode resultBlocks = toolResultTurn.putArray("content");

                for (JsonNode block : content) {
                    if (!"tool_use".equals(block.path("type").asText())) continue;
                    String toolName = block.path("name").asText();
                    String toolUseId = block.path("id").asText();
                    JsonNode input = block.path("input");

                    log.info("agent.tool_call name={} input={}", toolName, input);
                    sendEvent(emitter, "tool", toolEvent(toolName, input));

                    ObjectNode resultBlock = resultBlocks.addObject();
                    resultBlock.put("type", "tool_result");
                    resultBlock.put("tool_use_id", toolUseId);
                    try {
                        Object result = toolDefs.invoke(toolName, input);
                        resultBlock.put("content", mapper.writeValueAsString(result));
                        rec.toolCall(toolName, false);
                        // Surface the adoptable draft to the browser. The token lives only in the
                        // tool_result sent back to Claude otherwise; the frontend needs it to adopt
                        // the priced proposal into the cart.
                        if (result instanceof DraftCartDto draft) {
                            rec.draftCreated(draft.token());
                            sendEvent(emitter, "draft_cart", mapper.valueToTree(draft));
                        }
                    } catch (IllegalArgumentException e) {
                        resultBlock.put("content", "{\"error\":\"" + e.getMessage() + "\"}");
                        resultBlock.put("is_error", true);
                        rec.toolCall(toolName, true);
                    }
                }
                messages.add(toolResultTurn);
            }

            // Ran out of tool iterations without a final answer.
            log.warn("agent.tool_loop_exhausted iterations={}", props.maxToolIterations());
            emitError(emitter, "The assistant took too many steps. Please rephrase your request.");
        } finally {
            rec.commit();
        }
    }

    private ArrayNode buildInitialMessages(AgentChatRequest request) {
        ArrayNode messages = mapper.createArrayNode();
        for (AgentChatRequest.Message m : request.messages()) {
            ObjectNode msg = messages.addObject();
            msg.put("role", m.role());
            msg.put("content", m.content());
        }
        return messages;
    }

    private ObjectNode buildRequestBody(ArrayNode messages, ArrayNode toolSchemas) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", props.model());
        body.put("max_tokens", props.maxTokens());
        body.set("system", buildSystem());
        body.set("tools", toolSchemas);
        body.set("messages", messages);
        return body;
    }

    /**
     * System prompt as a content-block array so the (long, stable) catalog snapshot
     * can carry a cache_control breakpoint — repeated turns in a session hit the
     * prompt cache (PLAN §11.2).
     */
    private ArrayNode buildSystem() {
        ArrayNode system = mapper.createArrayNode();

        ObjectNode instructions = system.addObject();
        instructions.put("type", "text");
        instructions.put("text", SYSTEM_INSTRUCTIONS);

        ObjectNode catalog = system.addObject();
        catalog.put("type", "text");
        catalog.put("text", "Current catalog snapshot (authoritative — prices in paise):\n"
                + catalogSnapshot());
        catalog.putObject("cache_control").put("type", "ephemeral");

        return system;
    }

    /** Full in-stock catalog as compact JSON, for grounding + cache reuse. */
    private String catalogSnapshot() {
        List<AgentProductRef> all = tools.searchProducts("", List.of(), AgentTools.MAX_SEARCH_RESULTS);
        try {
            return mapper.writeValueAsString(all);
        } catch (Exception e) {
            log.warn("agent.catalog_snapshot_failed {}", e.getMessage());
            return "[]";
        }
    }

    private ObjectNode assistantTurn(JsonNode content) {
        ObjectNode turn = mapper.createObjectNode();
        turn.put("role", "assistant");
        turn.set("content", content);
        return turn;
    }

    private void emitAssistantText(SseEmitter emitter, JsonNode content) throws IOException {
        if (content == null || !content.isArray()) return;
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                String text = block.path("text").asText("");
                if (!text.isBlank()) {
                    ObjectNode ev = mapper.createObjectNode();
                    ev.put("text", text);
                    sendEvent(emitter, "message", ev);
                }
            }
        }
    }

    private ObjectNode toolEvent(String name, JsonNode input) {
        ObjectNode ev = mapper.createObjectNode();
        ev.put("name", name);
        ev.set("input", input);
        return ev;
    }

    private void emitError(SseEmitter emitter, String message) {
        try {
            ObjectNode ev = mapper.createObjectNode();
            ev.put("message", message);
            sendEvent(emitter, "error", ev);
        } catch (IOException ignored) {
            // Client likely disconnected; nothing more to do.
        }
    }

    private void sendEvent(SseEmitter emitter, String name, JsonNode data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(mapper.writeValueAsString(data)));
    }
}
