package com.corporate.dto;

import java.time.Instant;

/**
 * Admin summary view of an order — richer than OrderSummaryDto (customer +
 * payment status) but still light for a list page.
 */
public record AdminOrderSummaryDto(
        String orderNumber,
        String status,
        String paymentStatus,
        Instant paidAt,
        String paymentTerms,
        String invoiceNumber,
        long subtotalCents,
        int itemCount,
        String companyName,
        String contactName,
        String email,
        Instant createdAt
) {}
