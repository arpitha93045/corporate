package com.corporate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import com.corporate.entity.PaymentTerms;

public record CheckoutRequest(
        @NotNull @Valid Customer customer,
        @NotNull @Valid ShippingAddress shippingAddress,
        @NotEmpty @Valid List<Line> items,
        // Optional. Null defaults to IMMEDIATE (card) — keeps the existing request
        // shape valid. When NET_30, the service requires a non-blank poNumber.
        PaymentTerms paymentTerms,
        @Size(max = 80) String poNumber
) {
    public record Customer(
            @NotBlank String companyName,
            @NotBlank String contactName,
            @NotBlank @Email String email,
            String phone
    ) {}

    public record ShippingAddress(
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            String state,
            @NotBlank String postalCode,
            @NotBlank String country
    ) {}

    public record Line(
            @NotNull Long productId,
            @Min(1) int quantity,
            @Valid Branding branding
    ) {}

    /**
     * Optional per-line branding: a message/engraving and/or a logo URL. Both
     * fields are optional; an all-blank branding is treated as "no branding" by
     * the service. Validated at the boundary — logoUrl must be blank or http(s).
     */
    public record Branding(
            @Size(max = 500) String message,
            @Size(max = 1000)
            @Pattern(regexp = "^$|^https?://.*", message = "logo URL must be http(s)")
            String logoUrl
    ) {}
}
