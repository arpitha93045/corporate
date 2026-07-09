package com.corporate.controller;

import jakarta.validation.Valid;
import com.corporate.dto.AuthenticatedUser;
import com.corporate.dto.OrderDto;
import com.corporate.dto.OrderSummaryDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.corporate.dto.CheckoutRequest;
import com.corporate.service.CheckoutService;

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
                             @AuthenticationPrincipal AuthenticatedUser principal,
                             @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return checkout.placeOrder(req, principal.id(), idempotencyKey);
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
