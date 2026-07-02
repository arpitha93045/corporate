package org.example.corporate.auth;

import org.example.corporate.web.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setCompanyName(blankToNull(req.companyName()));
        user.setPhone(blankToNull(req.phone()));
        user.setRole(Role.CUSTOMER);
        AppUser saved = users.save(user);
        return token(saved);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return token(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserSummary me(Long userId) {
        AppUser u = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        return AuthDtos.UserSummary.from(u);
    }

    private AuthDtos.AuthResponse token(AppUser user) {
        return new AuthDtos.AuthResponse(
                jwt.issue(user),
                jwt.getTtlSeconds(),
                AuthDtos.UserSummary.from(user)
        );
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
