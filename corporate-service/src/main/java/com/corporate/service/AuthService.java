package com.corporate.service;

import com.corporate.web.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.corporate.dao.PasswordResetTokenRepository;
import com.corporate.dao.UserRepository;
import com.corporate.dto.AuthDtos;
import com.corporate.entity.AppUser;
import com.corporate.entity.PasswordResetToken;
import com.corporate.entity.Role;
import com.corporate.mail.MailService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mail;
    private final String baseUrl;

    public AuthService(UserRepository users, PasswordResetTokenRepository resetTokens,
                       PasswordEncoder encoder, JwtService jwt, MailService mail,
                       @Value("${app.base-url:http://localhost:4200}") String baseUrl) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            // Don't confirm whether the email is already registered — that lets
            // anyone enumerate accounts. Return the same generic error as any
            // other rejected registration. Note: this path returns faster than a
            // successful registration (which pays for BCrypt), so a determined
            // attacker with many samples could still enumerate via timing. The
            // per-IP rate limit (5/min) is the primary defence.
            throw new IllegalArgumentException("Could not create the account. Please check your details and try again.");
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

    /**
     * Issues a password reset token and emails the reset link. Always succeeds
     * from the caller's view — we never reveal whether the email is registered
     * (same anti-enumeration stance as {@link #register}). A user has at most one
     * usable token: issuing a new one deletes any prior tokens for that user.
     */
    @Transactional
    public void requestReset(AuthDtos.ForgotPasswordRequest req) {
        String email = req.email().trim().toLowerCase();
        var maybeUser = users.findByEmail(email);
        if (maybeUser.isEmpty()) {
            return;
        }
        AppUser user = maybeUser.get();
        resetTokens.deleteByUserId(user.getId());

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken(UUID.randomUUID().toString().replace("-", ""));
        prt.setExpiresAt(Instant.now().plus(RESET_TTL));
        resetTokens.save(prt);

        String link = baseUrl + "/reset-password?token=" + prt.getToken();
        mail.sendResetLink(user.getEmail(), link);
        log.info("password_reset.requested user_id={}", user.getId());
    }

    /**
     * Consumes a reset token and sets a new password. Rejects a missing, expired,
     * or already-used token. On success the token is stamped used (single-use);
     * the user then logs in normally with the new password.
     */
    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest req) {
        PasswordResetToken prt = resetTokens.findByToken(req.token())
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has expired."));

        AppUser user = prt.getUser();
        user.setPasswordHash(encoder.encode(req.password()));
        prt.setUsedAt(Instant.now());
        users.save(user);
        resetTokens.save(prt);
        log.info("password_reset.completed user_id={}", user.getId());
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
