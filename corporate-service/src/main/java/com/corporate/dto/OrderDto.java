package com.corporate.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import com.corporate.entity.OrderEntity;

public record OrderDto(
        String orderNumber,
        String status,
        String paymentStatus,
        Instant paidAt,
        String paymentTerms,
        String poNumber,
        String invoiceNumber,
        LocalDate dueDate,
        String companyName,
        String contactName,
        String email,
        String phone,
        Address shippingAddress,
        List<Item> items,
        long subtotalCents,
        Instant createdAt
) {
    public record Address(String line1, String line2, String city, String state, String postalCode, String country) {}
    public record Item(Long productId, String productName, long unitPriceCents, int quantity, long lineTotalCents,
                       String brandingMessage, String brandingLogoUrl) {}

    public static OrderDto from(OrderEntity o) {
        return new OrderDto(
                o.getOrderNumber(),
                o.getStatus().name(),
                o.getPaymentStatus(),
                o.getPaidAt(),
                o.getPaymentTerms().name(),
                o.getPoNumber(),
                o.getInvoiceNumber(),
                o.getDueDate(),
                o.getCompanyName(),
                o.getContactName(),
                o.getEmail(),
                o.getPhone(),
                new Address(
                        o.getAddressLine1(), o.getAddressLine2(),
                        o.getCity(), o.getState(),
                        o.getPostalCode(), o.getCountry()
                ),
                o.getItems().stream()
                        .map(i -> new Item(
                                i.getProduct().getId(),
                                i.getProductName(),
                                i.getUnitPriceCents(),
                                i.getQuantity(),
                                i.getLineTotalCents(),
                                i.getBrandingMessage(),
                                i.getBrandingLogoUrl()))
                        .toList(),
                o.getSubtotalCents(),
                o.getCreatedAt()
        );
    }
}
