package com.corporate.entity;

/**
 * How an order is paid. IMMEDIATE orders pay by card (Stripe) at checkout;
 * NET_30 orders are placed on invoice terms — a PO number is captured and the
 * buyer pays within 30 days, settled out-of-band and marked paid by an admin.
 */
public enum PaymentTerms {
    IMMEDIATE,
    NET_30
}
