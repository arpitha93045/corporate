package com.corporate.order;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import com.corporate.entity.OrderEntity;
import com.corporate.entity.OrderItem;
import com.corporate.entity.OrderStatus;
import com.corporate.mail.OrderMailFormatter;

class OrderMailFormatterTest {

    private final OrderMailFormatter formatter = new OrderMailFormatter();

    @Test
    void subjectIncludesOrderNumber() {
        OrderEntity o = sampleOrder();
        assertThat(formatter.subject(o)).isEqualTo("Payment received — order CG-2026-000123");
    }

    @Test
    void bodyContainsGreetingItemsTotalAndAddress() {
        OrderEntity o = sampleOrder();
        String body = formatter.body(o);

        assertThat(body).contains("Hi Priya,");
        assertThat(body).contains("CG-2026-000123");
        assertThat(body).contains("Assorted tea hamper");
        assertThat(body).contains("x2");
        // Two hampers at 149000 paise each => 298000 paise total => ₹2,980.
        assertThat(body).contains("₹2,980");
        assertThat(body).contains("Acme Corp");
        assertThat(body).contains("42 MG Road");
        assertThat(body).contains("Bengaluru");
        assertThat(body).contains("Karnataka");
        assertThat(body).contains("560001");
    }

    @Test
    void bodySkipsBlankAddressLine2AndState() {
        OrderEntity o = sampleOrder();
        o.setAddressLine2("");
        o.setState(null);
        String body = formatter.body(o);

        assertThat(body).doesNotContain("\n\n42 MG Road\n\n");
        // City appears followed by two spaces + postal code (no state comma).
        assertThat(body).contains("Bengaluru  560001");
    }

    private OrderEntity sampleOrder() {
        OrderEntity o = new OrderEntity();
        o.setOrderNumber("CG-2026-000123");
        o.setCompanyName("Acme Corp");
        o.setContactName("Priya");
        o.setEmail("priya@example.com");
        o.setAddressLine1("42 MG Road");
        o.setAddressLine2("Suite 5");
        o.setCity("Bengaluru");
        o.setState("Karnataka");
        o.setPostalCode("560001");
        o.setCountry("India");
        o.setSubtotalCents(298_000);
        o.setStatus(OrderStatus.PAID);
        o.setPaidAt(Instant.parse("2026-07-08T12:00:00Z"));

        OrderItem line = new OrderItem();
        line.setProductName("Assorted tea hamper");
        line.setUnitPriceCents(149_000);
        line.setQuantity(2);
        line.setLineTotalCents(298_000);
        o.addItem(line);
        return o;
    }
}
