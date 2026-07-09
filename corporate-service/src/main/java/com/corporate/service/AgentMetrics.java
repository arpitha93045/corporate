package com.corporate.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import com.corporate.dao.AgentChatMetricRepository;
import com.corporate.entity.AgentChatMetric;

/**
 * Agent observability (PLAN §11.4 step 6). Two backing stores, one API:
 * <ul>
 *   <li><b>Micrometer</b> — live counters/summaries at {@code /actuator/metrics}
 *       and {@code /actuator/prometheus}. Reset on restart; good for dashboards.</li>
 *   <li><b>agent_chat_metric</b> table — one durable row per chat turn, for
 *       historical rates (conversion / tokens-per-chat over a window).</li>
 * </ul>
 *
 * A caller opens a {@link ChatRecorder} for the turn, accumulates tool calls,
 * errors and token usage as the loop runs, notes any draft it produced, then
 * calls {@link ChatRecorder#commit()} once at the end — that fires the meters
 * and persists the row. Adoption is recorded separately when the buyer fetches
 * the draft by token.
 *
 * Metrics are best-effort: a failure here must never break a chat, so
 * {@code commit()} and {@code recordAdoption()} swallow their own errors.
 */
@Service
public class AgentMetrics {

    private static final Logger log = LoggerFactory.getLogger(AgentMetrics.class);

    private final MeterRegistry registry;
    private final AgentChatMetricRepository repo;
    private final TransactionTemplate txn;

    private final Counter chats;
    private final Counter draftsCreated;
    private final Counter draftsAdopted;
    private final DistributionSummary inputTokens;
    private final DistributionSummary outputTokens;
    private final DistributionSummary toolCallsPerChat;

    public AgentMetrics(MeterRegistry registry, AgentChatMetricRepository repo,
                        PlatformTransactionManager txManager) {
        this.registry = registry;
        this.repo = repo;
        // Metrics run on the SSE worker thread with no ambient transaction, and
        // adoption runs inside a readOnly fetch — either way we open our own.
        this.txn = new TransactionTemplate(txManager);
        this.txn.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.chats = Counter.builder("agent.chats")
                .description("Agent chat turns completed").register(registry);
        this.draftsCreated = Counter.builder("agent.drafts.created")
                .description("Chat turns that produced a priced draft cart").register(registry);
        this.draftsAdopted = Counter.builder("agent.drafts.adopted")
                .description("Draft carts fetched by token to adopt into the cart").register(registry);
        this.inputTokens = DistributionSummary.builder("agent.tokens.input")
                .description("Anthropic input tokens per chat turn").register(registry);
        this.outputTokens = DistributionSummary.builder("agent.tokens.output")
                .description("Anthropic output tokens per chat turn").register(registry);
        this.toolCallsPerChat = DistributionSummary.builder("agent.tool.calls.per_chat")
                .description("Tool calls per chat turn").register(registry);
    }

    /** Opens a per-turn accumulator. Not thread-safe; one per chat turn. */
    public ChatRecorder startChat() {
        return new ChatRecorder();
    }

    /**
     * Records that a draft was adopted (buyer fetched it by token). Bumps the
     * live counter and flips the durable row if we have one for that token.
     */
    public void recordAdoption(String draftToken) {
        try {
            draftsAdopted.increment();
            txn.executeWithoutResult(status -> repo.findByDraftToken(draftToken).ifPresent(row -> {
                if (!row.isAdopted()) {
                    row.setAdopted(true);
                    row.setAdoptedAt(Instant.now());
                    repo.save(row);
                }
            }));
        } catch (RuntimeException e) {
            log.warn("agent.metrics adoption record failed token={} err={}", draftToken, e.getMessage());
        }
    }

    /** Per-turn accumulator. Fill during the loop, {@link #commit()} once at the end. */
    public final class ChatRecorder {
        private int toolCalls;
        private int toolErrors;
        private long inTokens;
        private long outTokens;
        private String draftToken;

        private ChatRecorder() { }

        /** One tool invocation. {@code error} true if it returned is_error. */
        public void toolCall(String toolName, boolean error) {
            toolCalls++;
            Counter.builder("agent.tool.calls").tag("tool", toolName).register(registry).increment();
            if (error) {
                toolErrors++;
                Counter.builder("agent.tool.errors").tag("tool", toolName).register(registry).increment();
            }
        }

        /** Add one Claude response's token usage; called per loop iteration. */
        public void addTokens(long input, long output) {
            inTokens += input;
            outTokens += output;
        }

        /** Mark that this turn produced an adoptable draft cart. */
        public void draftCreated(String token) {
            this.draftToken = token;
        }

        /** Fire the meters and persist one durable row. Best-effort. */
        public void commit() {
            try {
                chats.increment();
                inputTokens.record(inTokens);
                outputTokens.record(outTokens);
                toolCallsPerChat.record(toolCalls);
                if (draftToken != null) draftsCreated.increment();

                txn.executeWithoutResult(status -> {
                    AgentChatMetric row = new AgentChatMetric();
                    row.setToolCalls(toolCalls);
                    row.setToolErrors(toolErrors);
                    row.setInputTokens(inTokens);
                    row.setOutputTokens(outTokens);
                    row.setProducedDraft(draftToken != null);
                    row.setDraftToken(draftToken);
                    repo.save(row);
                });
            } catch (RuntimeException e) {
                log.warn("agent.metrics commit failed err={}", e.getMessage());
            }
        }
    }
}
