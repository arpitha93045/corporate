package org.example.corporate.admin;

import org.example.corporate.auth.AppUser;
import org.example.corporate.auth.Role;
import org.example.corporate.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * On boot, promote the configured email to ADMIN if that user exists. Idempotent
 * — safe to run every startup. Only promotes; never demotes. The user must have
 * signed up first (this bootstrap does not create accounts, so it cannot leak
 * credentials into logs).
 *
 * Config: app.admin.email (env: APP_ADMIN_EMAIL). Empty = do nothing.
 */
@Configuration
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    ApplicationRunner promoteAdmin(UserRepository users,
                                   @Value("${app.admin.email:}") String adminEmail) {
        return args -> {
            if (adminEmail == null || adminEmail.isBlank()) return;
            promote(users, adminEmail.trim().toLowerCase());
        };
    }

    @Transactional
    void promote(UserRepository users, String email) {
        AppUser user = users.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("APP_ADMIN_EMAIL={} not found — sign up first, then restart to promote.", email);
            return;
        }
        if (user.getRole() == Role.ADMIN) return;
        user.setRole(Role.ADMIN);
        log.info("Promoted user {} to ADMIN.", email);
    }
}
