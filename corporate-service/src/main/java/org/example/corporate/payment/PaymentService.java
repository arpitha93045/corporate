package org.example.corporate.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.example.corporate.order.OrderEntity;
import org.example.corporate.order.OrderRepository;
import org.example.corporate.order.OrderStatus;
import org.example.corporate.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PaymentService {

    private final OrderRepository orderRepo;
    private final PaymentsConfig config;

    public PaymentService(OrderRepository orderRepo, PaymentsConfig config) {
        this.orderRepo = orderRepo;
        this.config = config;
    }

    /**
     * Create (or retrieve, if one already exists) a PaymentIntent for the given
     * order. Amount is always taken from the persisted order — never from the
     * client — so a tampered request cannot change what's charged.
     */
    @Transactional
    public PaymentIntentResponse createIntent(String orderNumber, Long userId) throws StripeException {
        OrderEntity order = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderNumber));
        if (order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new NotFoundException("Order not found: " + orderNumber);
        }
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Order " + orderNumber + " is not payable (status=" + order.getStatus() + ")");
        }

        if (order.getPaymentIntentId() != null) {
            PaymentIntent existing = PaymentIntent.retrieve(order.getPaymentIntentId());
            return new PaymentIntentResponse(existing.getId(), existing.getClientSecret());
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(order.getSubtotalCents())
                .setCurrency(config.currency())
                .setDescription("Order " + order.getOrderNumber())
                .putMetadata("order_number", order.getOrderNumber())
                .putMetadata("order_id", String.valueOf(order.getId()))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true).build())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        order.setPaymentIntentId(intent.getId());
        order.setPaymentStatus(intent.getStatus());
        return new PaymentIntentResponse(intent.getId(), intent.getClientSecret());
    }

    public record PaymentIntentResponse(String paymentIntentId, String clientSecret) {}
}
