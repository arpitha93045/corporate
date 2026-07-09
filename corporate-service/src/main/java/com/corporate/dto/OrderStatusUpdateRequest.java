package com.corporate.dto;

import jakarta.validation.constraints.NotNull;
import com.corporate.entity.OrderStatus;

public record OrderStatusUpdateRequest(@NotNull OrderStatus status) {}
