package org.example.corporate.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@ActiveProfiles("h2")
class RateLimitFilterTest {

    @Autowired WebApplicationContext ctx;
    @Autowired RateLimitFilter rateLimitFilter;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        rateLimitFilter.reset();
    }

    @Test
    void register_returns_429_after_burst_exhausted() throws Exception {
        // /api/auth/register is capped at 5/min per IP. First 5 requests must be
        // let through the filter; the 6th must be rejected with 429.
        for (int i = 0; i < 5; i++) {
            int status = mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerBody("burst" + i + "@example.com")))
                    .andReturn().getResponse().getStatus();
            // Registration itself might 200 or 400 depending on validation/dupe; we
            // only care that the filter didn't block us at this point.
            assertThat(status).isNotEqualTo(429);
        }

        var res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("burst-blocked@example.com")))
                .andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void login_and_register_limits_are_independent() throws Exception {
        // Exhaust the login bucket (10/min).
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"nobody@example.com","password":"wrongpass"}
                                    """))
                    .andReturn();
        }
        int blocked = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"wrongpass"}
                                """))
                .andReturn().getResponse().getStatus();
        assertThat(blocked).isEqualTo(429);

        // Register bucket should be untouched.
        int registerStatus = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("independent@example.com")))
                .andReturn().getResponse().getStatus();
        assertThat(registerStatus).isNotEqualTo(429);
    }

    private String registerBody(String email) {
        return """
                {"email":"%s","password":"hunter22!","fullName":"Test","companyName":"Acme","phone":""}
                """.formatted(email);
    }
}
