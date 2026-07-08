package org.example.corporate.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.example.corporate.order.OrderEntity;
import org.example.corporate.order.OrderRepository;
import org.example.corporate.order.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Stripe webhook endpoint. Public, but authenticated by the Stripe signature
 * header — never trust the body without verifying with the endpoint's secret.
 *
 * We deliberately keep this narrow: only payment_intent.succeeded and
 * payment_intent.payment_failed. Everything else is acked with 200 so Stripe
 * stops retrying. Idempotency comes for free — repeated succeeded events for
 * the same PI flip an already-PAID order to PAID again (a no-op set).
 */
@RestController
@RequestMapping("/api/payments")
class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentsConfig config;
    private final WebhookProcessor processor;

    StripeWebhookController(PaymentsConfig config, WebhookProcessor processor) {
        this.config = config;
        this.processor = processor;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, config.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Rejected Stripe webhook with bad signature");
            return ResponseEntity.status(400).body("bad signature");
        }

        StripeObject obj = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(obj instanceof PaymentIntent pi)) {
            return ResponseEntity.ok("ignored");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> processor.markPaid(pi);
            case "payment_intent.payment_failed" -> processor.markFailed(pi);
            default -> log.debug("Ignoring Stripe event {}", event.getType());
        }
        return ResponseEntity.ok("ok");
    }

    @Service
    static class WebhookProcessor {
        private final OrderRepository orderRepo;

        WebhookProcessor(OrderRepository orderRepo) {
            this.orderRepo = orderRepo;
        }

        @Transactional
        void markPaid(PaymentIntent pi) {
            OrderEntity order = orderRepo.findByPaymentIntentId(pi.getId()).orElse(null);
            if (order == null) {
                log.warn("Webhook succeeded for unknown PaymentIntent {}", pi.getId());
                return;
            }
            order.setPaymentStatus(pi.getStatus());
            if (order.getStatus() != OrderStatus.PAID) {
                order.setStatus(OrderStatus.PAID);
                order.setPaidAt(Instant.now());
                log.info("Order {} marked PAID (pi={})", order.getOrderNumber(), pi.getId());
            }
        }

        @Transactional
        void markFailed(PaymentIntent pi) {
            OrderEntity order = orderRepo.findByPaymentIntentId(pi.getId()).orElse(null);
            if (order == null) return;
            order.setPaymentStatus(pi.getStatus());
            log.info("Order {} payment failed (pi={}, status={})",
                    order.getOrderNumber(), pi.getId(), pi.getStatus());
        }
    }
}
