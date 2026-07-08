package org.example.corporate.order;

import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Renders the plain-text body used in transactional order emails. Pure
 * function of the order — no I/O, no formatting locale surprises: money is
 * always formatted as INR with the Indian grouping style, since the storefront
 * itself is Rupee-priced. Kept separate from MailService so it can be unit
 * tested without a JavaMailSender.
 */
@Component
public class OrderMailFormatter {

    private static final NumberFormat INR = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    static {
        INR.setMaximumFractionDigits(0);
    }

    public String subject(OrderEntity order) {
        return "Payment received — order " + order.getOrderNumber();
    }

    public String body(OrderEntity order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(order.getContactName()).append(",\n\n")
          .append("Thanks for your order with Corporate Gifting. ")
          .append("We've received your payment and your gifts are being prepared.\n\n")
          .append("Order: ").append(order.getOrderNumber()).append('\n');

        if (order.getPaidAt() != null) {
            sb.append("Paid at: ").append(order.getPaidAt()).append('\n');
        }
        sb.append('\n').append("Items\n").append("-----\n");

        for (OrderItem item : order.getItems()) {
            sb.append("- ").append(item.getProductName())
              .append("  x").append(item.getQuantity())
              .append("  ").append(formatMoney(item.getLineTotalCents()))
              .append('\n');
        }

        sb.append('\n')
          .append("Total: ").append(formatMoney(order.getSubtotalCents())).append('\n')
          .append('\n')
          .append("Shipping to\n")
          .append("-----------\n")
          .append(order.getCompanyName()).append('\n')
          .append(order.getContactName()).append('\n')
          .append(order.getAddressLine1()).append('\n');

        if (order.getAddressLine2() != null && !order.getAddressLine2().isBlank()) {
            sb.append(order.getAddressLine2()).append('\n');
        }
        sb.append(order.getCity());
        if (order.getState() != null && !order.getState().isBlank()) {
            sb.append(", ").append(order.getState());
        }
        sb.append("  ").append(order.getPostalCode()).append('\n')
          .append(order.getCountry()).append('\n')
          .append('\n')
          .append("We'll email again once your order ships.\n")
          .append("— Corporate Gifting\n");

        return sb.toString();
    }

    private static String formatMoney(long cents) {
        return INR.format(cents / 100.0);
    }
}
