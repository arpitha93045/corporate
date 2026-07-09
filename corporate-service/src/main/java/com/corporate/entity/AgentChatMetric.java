package com.corporate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One persisted row per completed agent chat turn (PLAN §11.4 step 6). Backs the
 * historical view of the same numbers Micrometer exposes live. A turn that ends
 * in a priced proposal sets {@code producedDraft} + {@code draftToken}; adoption
 * (the buyer fetching that draft by token) flips {@code adopted} + {@code adoptedAt}.
 */
@Entity
@Table(name = "agent_chat_metric")
public class AgentChatMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "tool_calls", nullable = false)
    private int toolCalls;

    @Column(name = "tool_errors", nullable = false)
    private int toolErrors;

    @Column(name = "input_tokens", nullable = false)
    private long inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "produced_draft", nullable = false)
    private boolean producedDraft;

    @Column(name = "draft_token")
    private String draftToken;

    @Column(name = "adopted", nullable = false)
    private boolean adopted;

    @Column(name = "adopted_at")
    private Instant adoptedAt;

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }

    public int getToolCalls() { return toolCalls; }
    public void setToolCalls(int v) { this.toolCalls = v; }

    public int getToolErrors() { return toolErrors; }
    public void setToolErrors(int v) { this.toolErrors = v; }

    public long getInputTokens() { return inputTokens; }
    public void setInputTokens(long v) { this.inputTokens = v; }

    public long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(long v) { this.outputTokens = v; }

    public boolean isProducedDraft() { return producedDraft; }
    public void setProducedDraft(boolean v) { this.producedDraft = v; }

    public String getDraftToken() { return draftToken; }
    public void setDraftToken(String v) { this.draftToken = v; }

    public boolean isAdopted() { return adopted; }
    public void setAdopted(boolean v) { this.adopted = v; }

    public Instant getAdoptedAt() { return adoptedAt; }
    public void setAdoptedAt(Instant v) { this.adoptedAt = v; }
}
