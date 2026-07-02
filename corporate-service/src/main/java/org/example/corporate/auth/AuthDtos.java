package org.example.corporate.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank String fullName,
            String companyName,
            String phone
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            long expiresInSeconds,
            UserSummary user
    ) {}

    public record UserSummary(
            Long id,
            String email,
            String fullName,
            String companyName,
            String phone,
            String role
    ) {
        public static UserSummary from(AppUser u) {
            return new UserSummary(
                    u.getId(), u.getEmail(), u.getFullName(),
                    u.getCompanyName(), u.getPhone(), u.getRole().name()
            );
        }
    }

    @Valid
    public interface Marker {}
}
