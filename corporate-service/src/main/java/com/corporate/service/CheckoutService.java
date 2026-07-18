package com.corporate.service;

import com.corporate.entity.Product;
import com.corporate.dao.ProductRepository;
import com.corporate.dto.OrderDto;
import com.corporate.entity.OrderEntity;
import com.corporate.entity.OrderItem;
import com.corporate.dao.OrderRepository;
import com.corporate.entity.OrderStatus;
import com.corporate.dto.OrderSummaryDto;
import com.corporate.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.corporate.dto.CheckoutRequest;
import com.corporate.web.InsufficientStockException;

@Service
public class CheckoutService {

    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;

    public CheckoutService(ProductRepository productRepo, OrderRepository orderRepo) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
    }

    @Transactional
    public OrderDto placeOrder(CheckoutRequest req, Long userId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepo.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                return OrderDto.from(existing.get());
            }
        }

        Map<GroupKey, Integer> mergedQuantities = new LinkedHashMap<>();
        for (CheckoutRequest.Line line : req.items()) {
            GroupKey key = GroupKey.of(line.productId(), line.branding());
            mergedQuantities.merge(key, line.quantity(), Integer::sum);
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PLACED);
        order.setUserId(userId);
        order.setIdempotencyKey(
                (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : null);

        order.setCompanyName(req.customer().companyName());
        order.setContactName(req.customer().contactName());
        order.setEmail(req.customer().email());
        order.setPhone(req.customer().phone());

        order.setAddressLine1(req.shippingAddress().line1());
        order.setAddressLine2(req.shippingAddress().line2());
        order.setCity(req.shippingAddress().city());
        order.setState(req.shippingAddress().state());
        order.setPostalCode(req.shippingAddress().postalCode());
        order.setCountry(req.shippingAddress().country());

        // A product may appear in several branding groups. Stock must be checked
        // and decremented once per product against the combined quantity, or two
        // branded groups could each pass a check they'd jointly fail.
        Map<Long, Integer> totalByProduct = new HashMap<>();
        for (Map.Entry<GroupKey, Integer> entry : mergedQuantities.entrySet()) {
            totalByProduct.merge(entry.getKey().productId(), entry.getValue(), Integer::sum);
        }

        Map<Long, Product> locked = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : totalByProduct.entrySet()) {
            Long productId = entry.getKey();
            int totalQty = entry.getValue();

            Product product = productRepo.findByIdForUpdate(productId)
                    .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

            if (product.getStockQuantity() < totalQty) {
                throw new InsufficientStockException(
                        "Not enough stock for %s (requested %d, available %d)"
                                .formatted(product.getName(), totalQty, product.getStockQuantity()));
            }
            product.setStockQuantity(product.getStockQuantity() - totalQty);
            locked.put(productId, product);
        }

        long subtotal = 0L;
        for (Map.Entry<GroupKey, Integer> entry : mergedQuantities.entrySet()) {
            GroupKey key = entry.getKey();
            int qty = entry.getValue();
            Product product = locked.get(key.productId());

            long unitPrice = product.getPriceCents();
            long lineTotal = unitPrice * qty;

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPriceCents(unitPrice);
            item.setQuantity(qty);
            item.setLineTotalCents(lineTotal);
            item.setBrandingMessage(key.message());
            item.setBrandingLogoUrl(key.logoUrl());
            order.addItem(item);

            subtotal += lineTotal;
        }
        order.setSubtotalCents(subtotal);

        OrderEntity saved = orderRepo.save(order);
        return OrderDto.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto getByOrderNumber(String orderNumber, Long userId) {
        OrderEntity order = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderNumber));
        if (order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new NotFoundException("Order not found: " + orderNumber);
        }
        return OrderDto.from(order);
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderSummaryDto> listForUser(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderSummaryDto::from)
                .toList();
    }

    private String generateOrderNumber() {
        long seq = orderRepo.nextOrderNumberSeq();
        return "CG-%d-%06d".formatted(Year.now().getValue(), seq);
    }

    /**
     * Merge key for order lines: same product + same normalized branding merge
     * into one order item; differing branding stays separate. Blank branding
     * fields normalize to null so empty UI inputs don't split a line.
     */
    private record GroupKey(Long productId, String message, String logoUrl) {
        static GroupKey of(Long productId, CheckoutRequest.Branding branding) {
            if (branding == null) {
                return new GroupKey(productId, null, null);
            }
            return new GroupKey(productId, normalize(branding.message()), normalize(branding.logoUrl()));
        }

        private static String normalize(String s) {
            if (s == null) return null;
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
