package org.example.corporate.admin;

import org.example.corporate.order.OrderDto;
import org.example.corporate.order.OrderEntity;
import org.example.corporate.order.OrderItem;
import org.example.corporate.order.OrderRepository;
import org.example.corporate.order.OrderStatus;
import org.example.corporate.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminOrderService {

    /**
     * Allowed status transitions. Kept deliberately narrow so an admin can't
     * accidentally revive a CANCELLED order or mark something FULFILLED before
     * it's PAID. Payments flip PLACED → PAID (via webhook), so admins mainly
     * move PAID → FULFILLED or cancel a stuck PLACED.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PLACED,   EnumSet.of(OrderStatus.CANCELLED),
            OrderStatus.PAID,     EnumSet.of(OrderStatus.FULFILLED, OrderStatus.CANCELLED),
            OrderStatus.FULFILLED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepo;

    public AdminOrderService(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderSummaryDto> listAll() {
        return orderRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> new AdminOrderSummaryDto(
                        o.getOrderNumber(),
                        o.getStatus().name(),
                        o.getPaymentStatus(),
                        o.getPaidAt(),
                        o.getSubtotalCents(),
                        o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                        o.getCompanyName(),
                        o.getContactName(),
                        o.getEmail(),
                        o.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(String orderNumber) {
        OrderEntity o = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderNumber));
        return OrderDto.from(o);
    }

    @Transactional
    public OrderDto updateStatus(String orderNumber, OrderStatus target) {
        OrderEntity o = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderNumber));
        OrderStatus current = o.getStatus();
        if (current == target) return OrderDto.from(o);
        Set<OrderStatus> allowed = ALLOWED.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalStateException(
                    "Cannot transition order " + orderNumber + " from " + current + " to " + target);
        }
        o.setStatus(target);
        return OrderDto.from(o);
    }
}
