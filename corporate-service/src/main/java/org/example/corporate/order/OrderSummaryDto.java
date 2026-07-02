package org.example.corporate.order;

import java.time.Instant;

public record OrderSummaryDto(
        String orderNumber,
        String status,
        long subtotalCents,
        int itemCount,
        Instant createdAt
) {
    public static OrderSummaryDto from(OrderEntity o) {
        return new OrderSummaryDto(
                o.getOrderNumber(),
                o.getStatus().name(),
                o.getSubtotalCents(),
                o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                o.getCreatedAt()
        );
    }
}
