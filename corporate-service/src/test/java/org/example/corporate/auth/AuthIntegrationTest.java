package org.example.corporate.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.corporate.web.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("h2")
class AuthIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder encoder;
    @Autowired RateLimitFilter rateLimitFilter;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        rateLimitFilter.reset();
    }

    @Test
    void rejects_password_longer_than_72_chars() throws Exception {
        String password73 = "a".repeat(73);
        int status = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("longpw-" + suffix() + "@example.com", password73)))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(400);
    }

    @Test
    void accepts_password_at_exactly_72_chars() throws Exception {
        String password72 = "a".repeat(72);
        int status = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("pw72-" + suffix() + "@example.com", password72)))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    void duplicate_email_returns_generic_message() throws Exception {
        String email = "dupe-" + suffix() + "@example.com";
        String password = "hunter22!";

        int first = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, password)))
                .andReturn().getResponse().getStatus();
        assertThat(first).isEqualTo(200);

        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, password)))
                .andReturn().getResponse().getContentAsString();

        String msg = mapper.readTree(body).get("message").asText();
        assertThat(msg).doesNotContainIgnoringCase("already");
        assertThat(msg).doesNotContainIgnoringCase("exists");
    }

    @Test
    void bcrypt_at_cost_12_verifies_hash_produced_at_cost_10() {
        // BCrypt's design: the cost is embedded in the hash prefix. A cost-12
        // encoder verifies cost-10 hashes just fine. This guards against a
        // silent regression if someone changes the encoder to a non-BCrypt one
        // or wires up multiple encoders inconsistently.
        String password = "hunter22!";
        String hashAtCost10 = new BCryptPasswordEncoder(10).encode(password);

        assertThat(encoder.matches(password, hashAtCost10)).isTrue();
        assertThat(hashAtCost10).startsWith("$2a$10$");
    }

    private String registerBody(String email, String password) {
        return """
                {"email":"%s","password":"%s","fullName":"Test","companyName":"Acme","phone":""}
                """.formatted(email, password);
    }

    private String suffix() {
        return Long.toString(System.nanoTime(), 36);
    }
}
