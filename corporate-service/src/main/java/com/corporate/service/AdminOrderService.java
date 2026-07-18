package com.corporate.service;

import com.corporate.dto.OrderDto;
import com.corporate.entity.OrderEntity;
import com.corporate.entity.OrderItem;
import com.corporate.dao.OrderRepository;
import com.corporate.entity.OrderStatus;
import com.corporate.entity.PaymentTerms;
import com.corporate.mail.MailService;
import com.corporate.mail.OrderMailFormatter;
import com.corporate.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.corporate.dto.AdminOrderSummaryDto;

@Service
public class AdminOrderService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderService.class);

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
    private final MailService mail;
    private final OrderMailFormatter mailFormatter;

    public AdminOrderService(OrderRepository orderRepo, MailService mail, OrderMailFormatter mailFormatter) {
        this.orderRepo = orderRepo;
        this.mail = mail;
        this.mailFormatter = mailFormatter;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderSummaryDto> listAll() {
        return orderRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> new AdminOrderSummaryDto(
                        o.getOrderNumber(),
                        o.getStatus().name(),
                        o.getPaymentStatus(),
                        o.getPaidAt(),
                        o.getPaymentTerms().name(),
                        o.getInvoiceNumber(),
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

    /**
     * Mark a net-30 (invoice) order paid. Net-30 orders never touch Stripe, so
     * the webhook path that normally flips PLACED -> PAID and emails the buyer
     * doesn't apply — this does the same work when an admin confirms the invoice
     * cleared. Guarded to NET_30 + PLACED so it can't touch a card order or
     * double-pay one already settled.
     */
    @Transactional
    public OrderDto markInvoicePaid(String orderNumber) {
        OrderEntity o = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderNumber));
        if (o.getPaymentTerms() != PaymentTerms.NET_30) {
            throw new IllegalStateException("Order " + orderNumber + " is not a net-30 invoice order.");
        }
        if (o.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException(
                    "Invoice for order " + orderNumber + " cannot be marked paid from status " + o.getStatus());
        }
        o.setStatus(OrderStatus.PAID);
        o.setPaidAt(Instant.now());
        o.setPaymentStatus("PAID");
        log.info("Order {} invoice marked PAID (invoice={})", o.getOrderNumber(), o.getInvoiceNumber());
        mail.sendIfEnabled(o.getEmail(), mailFormatter.subject(o), mailFormatter.body(o));
        return OrderDto.from(o);
    }
}
