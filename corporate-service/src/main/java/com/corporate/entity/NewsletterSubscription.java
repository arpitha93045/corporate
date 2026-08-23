package com.corporate.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "newsletter_subscription")
public class NewsletterSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "subscribed_at", nullable = false)
    private Instant subscribedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "unsubscribed_at")
    private Instant unsubscribedAt;

    public NewsletterSubscription() {}

    public NewsletterSubscription(String email) {
        this.email = email.toLowerCase().trim();
        this.subscribedAt = Instant.now();
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(Instant subscribedAt) { this.subscribedAt = subscribedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getUnsubscribedAt() { return unsubscribedAt; }
    public void setUnsubscribedAt(Instant unsubscribedAt) { this.unsubscribedAt = unsubscribedAt; }
}

