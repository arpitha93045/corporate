package com.corporate.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import com.corporate.service.JwtService;

/**
 * Wires the Stripe SDK's global API key and guards against booting on the
 * prod profile with the sentinel placeholder key. Same pattern as JwtService.
 */
@Configuration
public class PaymentsConfig {

    static final String DEV_PLACEHOLDER_SECRET = "sk_test_replace_me";
    static final String DEV_PLACEHOLDER_WEBHOOK = "whsec_replace_me";

    private final String secretKey;
    private final String webhookSecret;
    private final String currency;
    private final Environment env;

    public PaymentsConfig(
            @Value("${app.stripe.secret-key:" + DEV_PLACEHOLDER_SECRET + "}") String secretKey,
            @Value("${app.stripe.webhook-secret:" + DEV_PLACEHOLDER_WEBHOOK + "}") String webhookSecret,
            @Value("${app.stripe.currency:inr}") String currency,
            Environment env
    ) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.currency = currency;
        this.env = env;
    }

    @PostConstruct
    void init() {
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (isProd) {
            if (DEV_PLACEHOLDER_SECRET.equals(secretKey)) {
                throw new IllegalStateException(
                        "Refusing to start with the placeholder Stripe secret key on the 'prod' profile. "
                                + "Set STRIPE_SECRET_KEY to a real sk_live_ or sk_test_ key.");
            }
            if (DEV_PLACEHOLDER_WEBHOOK.equals(webhookSecret)) {
                throw new IllegalStateException(
                        "Refusing to start with the placeholder Stripe webhook secret on the 'prod' profile. "
                                + "Set STRIPE_WEBHOOK_SECRET to the value from your webhook endpoint.");
            }
        }
        Stripe.apiKey = secretKey;
    }

    public String webhookSecret() { return webhookSecret; }
    public String currency() { return currency; }
}
