package com.corporate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 200) String slug
) {}
