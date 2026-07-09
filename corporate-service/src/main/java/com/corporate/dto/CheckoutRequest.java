package com.corporate.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequest(
        @NotNull @Valid Customer customer,
        @NotNull @Valid ShippingAddress shippingAddress,
        @NotEmpty @Valid List<Line> items
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
            @Min(1) int quantity
    ) {}
}
