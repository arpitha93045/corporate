package org.example.corporate.payment;

import com.stripe.exception.StripeException;
import org.example.corporate.auth.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/intent/{orderNumber}")
    public PaymentService.PaymentIntentResponse createIntent(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal AuthenticatedUser principal) throws StripeException {
        return payments.createIntent(orderNumber, principal.id());
    }
}
