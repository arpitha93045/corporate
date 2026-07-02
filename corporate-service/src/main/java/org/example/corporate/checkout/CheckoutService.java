package org.example.corporate.checkout;

import org.example.corporate.catalog.Product;
import org.example.corporate.catalog.ProductRepository;
import org.example.corporate.order.OrderDto;
import org.example.corporate.order.OrderEntity;
import org.example.corporate.order.OrderItem;
import org.example.corporate.order.OrderRepository;
import org.example.corporate.order.OrderStatus;
import org.example.corporate.order.OrderSummaryDto;
import org.example.corporate.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CheckoutService {

    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;

    public CheckoutService(ProductRepository productRepo, OrderRepository orderRepo) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
    }

    @Transactional
    public OrderDto placeOrder(CheckoutRequest req, Long userId) {
        Map<Long, Integer> mergedQuantities = new HashMap<>();
        for (CheckoutRequest.Line line : req.items()) {
            mergedQuantities.merge(line.productId(), line.quantity(), Integer::sum);
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PLACED);
        order.setUserId(userId);

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

        long subtotal = 0L;
        for (Map.Entry<Long, Integer> entry : mergedQuantities.entrySet()) {
            Long productId = entry.getKey();
            int qty = entry.getValue();

            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

            if (!product.isInStock()) {
                throw new IllegalArgumentException("Product is out of stock: " + product.getName());
            }

            long unitPrice = product.getPriceCents();
            long lineTotal = unitPrice * qty;

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnitPriceCents(unitPrice);
            item.setQuantity(qty);
            item.setLineTotalCents(lineTotal);
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
        int year = Year.now().getValue();
        int suffix = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        return "CG-%d-%06d".formatted(year, suffix);
    }
}
