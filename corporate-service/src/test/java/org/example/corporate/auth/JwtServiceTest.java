package org.example.corporate.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String CUSTOM_PROD_SECRET =
            "a-different-secret-that-is-clearly-at-least-32-bytes-long";

    @Test
    void refuses_to_boot_on_prod_profile_with_dev_default_secret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtService(JwtService.DEV_DEFAULT_SECRET, 3600L, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void boots_on_dev_profile_with_default_secret() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        assertThatCode(() -> new JwtService(JwtService.DEV_DEFAULT_SECRET, 3600L, env))
                .doesNotThrowAnyException();
    }

    @Test
    void boots_on_prod_profile_when_secret_is_customised() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        JwtService svc = new JwtService(CUSTOM_PROD_SECRET, 3600L, env);
        assertThat(svc.getTtlSeconds()).isEqualTo(3600L);
    }

    @Test
    void rejects_secret_shorter_than_32_bytes_regardless_of_profile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");

        assertThatThrownBy(() -> new JwtService("too-short", 3600L, env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
