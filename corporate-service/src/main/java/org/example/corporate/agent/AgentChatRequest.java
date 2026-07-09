package org.example.corporate.agent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * A chat turn from the client. Conversation state is stateless for this slice:
 * the browser holds the history and replays the full message list on each POST
 * (per PLAN §11.5 — catalog + current chat only, no server-side persistence yet).
 */
public record AgentChatRequest(
        @NotEmpty(message = "messages must not be empty")
        @Valid
        List<Message> messages
) {
    public record Message(
            @NotBlank
            @Pattern(regexp = "user|assistant", message = "role must be 'user' or 'assistant'")
            String role,
            @NotBlank(message = "content must not be blank")
            String content
    ) {}
}
