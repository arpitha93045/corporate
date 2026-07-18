package com.corporate.controller;

import com.corporate.dto.QuoteDto;
import com.corporate.service.QuoteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, token-addressed access to a quote. The opaque token is the capability
 * (no auth) — same model as the agent draft cart. The buyer views the quote and
 * accepts or declines it. Quotes are created only by admins, so there is no POST
 * to create one here.
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService service;

    public QuoteController(QuoteService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    public QuoteDto get(@PathVariable String token) {
        return service.fetchByToken(token);
    }

    @PostMapping("/{token}/accept")
    public QuoteDto accept(@PathVariable String token) {
        return service.respond(token, true);
    }

    @PostMapping("/{token}/decline")
    public QuoteDto decline(@PathVariable String token) {
        return service.respond(token, false);
    }
}
