-- V10: Durable per-chat metrics for the AI Gifting Agent (PLAN §11.4 step 6).
--
-- One row per completed chat turn. Live counters/timers also flow to Micrometer
-- (/actuator/metrics), but those reset on restart; this table backs historical
-- questions like "conversion rate this week" and "avg tokens/chat this month".
--
-- Conversion signal: produced_draft marks a turn that ended in a priced draft;
-- adopted flips true when the buyer fetches that draft by token to add it to the
-- cart (GET /api/agent/draft-cart/{token}). "% of chats ending in checkout" is
-- approximated as adopted / total — the buyer still completes the real checkout.

CREATE TABLE agent_chat_metric (
    id             BIGSERIAL   PRIMARY KEY,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tool_calls     INTEGER     NOT NULL DEFAULT 0,
    tool_errors    INTEGER     NOT NULL DEFAULT 0,
    input_tokens   BIGINT      NOT NULL DEFAULT 0,
    output_tokens  BIGINT      NOT NULL DEFAULT 0,
    produced_draft BOOLEAN     NOT NULL DEFAULT FALSE,
    draft_token    VARCHAR(40),
    adopted        BOOLEAN     NOT NULL DEFAULT FALSE,
    adopted_at     TIMESTAMP
);

-- Adoption lookup flips the row by the draft's token.
CREATE INDEX ix_agent_chat_metric_draft_token ON agent_chat_metric (draft_token);
-- Time-window aggregates (conversion/tokens over a period).
CREATE INDEX ix_agent_chat_metric_created_at ON agent_chat_metric (created_at);
