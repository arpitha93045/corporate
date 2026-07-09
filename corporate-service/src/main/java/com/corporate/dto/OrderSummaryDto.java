package com.corporate.dto;

import java.time.Instant;
import com.corporate.entity.OrderEntity;
import com.corporate.entity.OrderItem;

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
