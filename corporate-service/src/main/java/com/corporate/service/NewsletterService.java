package com.corporate.service;

import com.corporate.dao.NewsletterSubscriptionRepository;
import com.corporate.entity.NewsletterSubscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NewsletterService {

    private final NewsletterSubscriptionRepository repository;

    public NewsletterService(NewsletterSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NewsletterSubscription subscribe(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        return repository.findByEmail(normalizedEmail)
            .map(existing -> {
                // Reactivate if previously unsubscribed
                if (!existing.isActive()) {
                    existing.setActive(true);
                    existing.setUnsubscribedAt(null);
                    return repository.save(existing);
                }
                return existing;
            })
            .orElseGet(() -> repository.save(new NewsletterSubscription(normalizedEmail)));
    }

    @Transactional
    public boolean unsubscribe(String email) {
        String normalizedEmail = email.toLowerCase().trim();

        return repository.findByEmail(normalizedEmail)
            .map(sub -> {
                if (sub.isActive()) {
                    sub.setActive(false);
                    sub.setUnsubscribedAt(Instant.now());
                    repository.save(sub);
                    return true;
                }
                return false;
            })
            .orElse(false);
    }

    public boolean isSubscribed(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        return repository.findByEmail(normalizedEmail)
            .map(NewsletterSubscription::isActive)
            .orElse(false);
    }

    public long getActiveSubscriberCount() {
        return repository.countByActiveTrue();
    }
}

