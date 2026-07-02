package org.example.corporate.order;

import java.time.Instant;
import java.util.List;

public record OrderDto(
        String orderNumber,
        String status,
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
    public record Item(Long productId, String productName, long unitPriceCents, int quantity, long lineTotalCents) {}

    public static OrderDto from(OrderEntity o) {
        return new OrderDto(
                o.getOrderNumber(),
                o.getStatus().name(),
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
                                i.getLineTotalCents()))
                        .toList(),
                o.getSubtotalCents(),
                o.getCreatedAt()
        );
    }
}
