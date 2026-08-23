package com.corporate.controller;

import com.corporate.service.NewsletterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    public record SubscribeRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        String email
    ) {}

    public record SubscribeResponse(
        boolean success,
        String message
    ) {}

    @PostMapping("/subscribe")
    public ResponseEntity<SubscribeResponse> subscribe(@Valid @RequestBody SubscribeRequest request) {
        newsletterService.subscribe(request.email());
        return ResponseEntity.ok(new SubscribeResponse(
            true,
            "Thank you for subscribing! You'll receive our latest offers and updates."
        ));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<SubscribeResponse> unsubscribe(@Valid @RequestBody SubscribeRequest request) {
        boolean unsubscribed = newsletterService.unsubscribe(request.email());
        if (unsubscribed) {
            return ResponseEntity.ok(new SubscribeResponse(
                true,
                "You have been successfully unsubscribed."
            ));
        }
        return ResponseEntity.ok(new SubscribeResponse(
            false,
            "This email is not subscribed to our newsletter."
        ));
    }
}

