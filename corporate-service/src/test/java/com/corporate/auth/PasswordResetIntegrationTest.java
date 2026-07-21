package com.corporate.auth;

import com.corporate.dao.PasswordResetTokenRepository;
import com.corporate.dao.UserRepository;
import com.corporate.entity.PasswordResetToken;
import com.corporate.web.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("h2")
class PasswordResetIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired RateLimitFilter rateLimitFilter;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        rateLimitFilter.reset();
    }

    @Test
    void forgot_for_unknown_email_is_ok_and_creates_no_token() throws Exception {
        long before = resetTokens.count();
        int status = forgot("nobody-" + suffix() + "@example.com");
        assertThat(status).isEqualTo(200);
        assertThat(resetTokens.count()).isEqualTo(before);
    }

    @Test
    void forgot_for_known_email_creates_exactly_one_usable_token_and_reissue_replaces_it() throws Exception {
        String email = register();
        Long userId = users.findByEmail(email).orElseThrow().getId();

        assertThat(forgot(email)).isEqualTo(200);
        var afterFirst = resetTokens.findByUserId(userId);
        assertThat(afterFirst).hasSize(1);
        assertThat(afterFirst.get(0).isUsable()).isTrue();
        String firstToken = afterFirst.get(0).getToken();

        assertThat(forgot(email)).isEqualTo(200);
        var afterSecond = resetTokens.findByUserId(userId);
        assertThat(afterSecond).hasSize(1);
        assertThat(afterSecond.get(0).getToken()).isNotEqualTo(firstToken);
    }

    @Test
    void full_reset_flow_changes_password_and_token_is_single_use() throws Exception {
        String email = register();
        Long userId = users.findByEmail(email).orElseThrow().getId();
        String oldHash = users.findByEmail(email).orElseThrow().getPasswordHash();

        forgot(email);
        String token = resetTokens.findByUserId(userId).get(0).getToken();

        assertThat(reset(token, "brand-new-pass9")).isEqualTo(200);

        String newHash = users.findByEmail(email).orElseThrow().getPasswordHash();
        assertThat(newHash).isNotEqualTo(oldHash);
        assertThat(resetTokens.findByToken(token).orElseThrow().getUsedAt()).isNotNull();

        // New password logs in, old one no longer works.
        assertThat(login(email, "brand-new-pass9")).isEqualTo(200);
        assertThat(login(email, "original-pass1")).isEqualTo(400);

        // Reusing the same token is rejected.
        assertThat(reset(token, "another-pass99")).isEqualTo(400);
    }

    @Test
    void reset_with_unknown_token_is_rejected() throws Exception {
        assertThat(reset("does-not-exist", "whatever-pass1")).isEqualTo(400);
    }

    @Test
    void reset_with_expired_token_is_rejected() throws Exception {
        String email = register();
        Long userId = users.findByEmail(email).orElseThrow().getId();
        forgot(email);
        PasswordResetToken t = resetTokens.findByUserId(userId).get(0);
        t.setExpiresAt(Instant.now().minusSeconds(60));
        resetTokens.save(t);

        assertThat(reset(t.getToken(), "brand-new-pass9")).isEqualTo(400);
    }

    private String register() throws Exception {
        String email = "reset-" + suffix() + "@example.com";
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"original-pass1","fullName":"Test","companyName":"Acme","phone":""}
                        """.formatted(email)));
        return email;
    }

    private int forgot(String email) throws Exception {
        return mvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andReturn().getResponse().getStatus();
    }

    private int reset(String token, String password) throws Exception {
        return mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"password\":\"%s\"}".formatted(token, password)))
                .andReturn().getResponse().getStatus();
    }

    private int login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn().getResponse().getStatus();
    }

    private String suffix() {
        return Long.toString(System.nanoTime(), 36);
    }
}
