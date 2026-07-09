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
