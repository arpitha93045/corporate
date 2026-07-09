package org.example.corporate.agent;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI Gifting Agent chat endpoint. Anonymous (per-IP rate limited via
 * RateLimitFilter) and propose-only — see PLAN §11.4/§11.5.
 *
 * Streams the turn as Server-Sent Events. The disabled/no-key case is checked
 * synchronously so it surfaces as a clean 503 (via GlobalExceptionHandler)
 * before the SSE stream opens; failures once streaming has begun arrive as an
 * SSE {@code error} event instead.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final long STREAM_TIMEOUT_MS = 120_000L;

    private final AgentChatService chatService;
    private final AgentProperties props;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public AgentController(AgentChatService chatService, AgentProperties props) {
        this.chatService = chatService;
        this.props = props;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AgentChatRequest request) {
        if (!props.isUsable()) {
            // Fail fast with a normal HTTP status before opening the stream.
            throw new AgentDisabledException("AI gifting assistant is not enabled");
        }
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        executor.execute(() -> chatService.stream(request, emitter));
        return emitter;
    }
}
