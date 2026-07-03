package org.example.corporate.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@ActiveProfiles("h2")
class ActuatorHealthTest {

    @Autowired WebApplicationContext ctx;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    @Test
    void health_is_anonymous_and_returns_bare_status() throws Exception {
        String body = mvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getContentAsString();

        // Bare {"status":"UP"} — no components, no details leaked to anon callers.
        assertThat(body).contains("\"status\"");
        assertThat(body).doesNotContain("components");
        assertThat(body).doesNotContain("diskSpace");
        assertThat(body).doesNotContain("db");
    }

    @Test
    void other_actuator_endpoints_are_not_exposed() throws Exception {
        int envStatus = mvc.perform(get("/actuator/env")).andReturn().getResponse().getStatus();
        int beansStatus = mvc.perform(get("/actuator/beans")).andReturn().getResponse().getStatus();

        // Neither should be a 200 — Spring returns 404 for un-exposed endpoints.
        assertThat(envStatus).isNotEqualTo(200);
        assertThat(beansStatus).isNotEqualTo(200);
    }
}
