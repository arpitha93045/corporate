package org.example.corporate.admin;

import jakarta.validation.constraints.NotNull;
import org.example.corporate.order.OrderStatus;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {}
