package org.example.corporate.agent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only access to an agent-created draft cart by its opaque token. The
 * frontend calls this to adopt the agent's proposal into the client-side cart.
 * The token is the capability — no auth beyond possessing it. Drafts are created
 * only via the agent tool, so there is deliberately no POST here.
 */
@RestController
@RequestMapping("/api/agent/draft-cart")
public class DraftCartController {

    private final DraftCartService service;

    public DraftCartController(DraftCartService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public DraftCartDto get(@PathVariable String token) {
        return service.fetch(token);
    }
}
