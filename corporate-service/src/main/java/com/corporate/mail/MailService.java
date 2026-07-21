package com.corporate.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;

    public MailService(
            JavaMailSender sender,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:no-reply@corporate-gifting.local}") String from
    ) {
        this.sender = sender;
        this.enabled = enabled;
        this.from = from;
    }

    /**
     * Sends a password-reset link. When mail is disabled (dev/h2) the link is
     * logged so the flow is testable without SMTP; the link is deliberately
     * NEVER logged when mail is enabled, so a real deployment can't leak reset
     * tokens into logs.
     */
    public void sendResetLink(String to, String link) {
        if (!enabled) {
            log.info("Mail disabled; password reset link for {} -> {}", to, link);
            return;
        }
        String body = """
                We received a request to reset the password for your Corporate Gifting account.

                Reset your password using the link below. It expires in 1 hour and can be used once.

                %s

                If you didn't request this, you can safely ignore this email — your password won't change.
                """.formatted(link);
        sendIfEnabled(to, "Reset your Corporate Gifting password", body);
    }

    public void sendIfEnabled(String to, String subject, String body) {
        if (!enabled) {
            log.info("Mail disabled; would have sent to={} subject={}", to, subject);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
            log.info("Mail sent to={} subject={}", to, subject);
        } catch (Exception e) {
            log.warn("Mail send failed to={} subject={}: {}", to, subject, e.getMessage());
        }
    }
}
