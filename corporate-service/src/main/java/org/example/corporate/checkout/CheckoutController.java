package org.example.corporate.checkout;

import jakarta.validation.Valid;
import org.example.corporate.auth.AuthenticatedUser;
import org.example.corporate.order.OrderDto;
import org.example.corporate.order.OrderSummaryDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@Valid @RequestBody CheckoutRequest req,
                             @AuthenticationPrincipal AuthenticatedUser principal) {
        return checkout.placeOrder(req, principal.id());
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderDto get(@PathVariable String orderNumber,
                        @AuthenticationPrincipal AuthenticatedUser principal) {
        return checkout.getByOrderNumber(orderNumber, principal.id());
    }

    @GetMapping("/orders")
    public List<OrderSummaryDto> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return checkout.listForUser(principal.id());
    }
}
