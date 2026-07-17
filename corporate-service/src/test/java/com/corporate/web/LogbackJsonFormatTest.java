package com.corporate.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the hand-rolled JSON pattern used by the prod appender in
 * logback-spring.xml. The risk with a string-built JSON layout is that a
 * quote or backslash in the log message breaks the line and silently corrupts
 * log ingestion; the %replace in the pattern is there to prevent that.
 *
 * We test the pattern directly with a PatternLayout rather than booting the
 * full logback-spring.xml, because {@code <springProfile>} is resolved by
 * Spring Boot's logging initializer, not by plain logback — so the XML's
 * profile blocks only activate inside a running Boot app. Keeping the pattern
 * string here in sync with the XML is the tradeoff; if they diverge, this test
 * still proves the escaping approach is sound.
 */
class LogbackJsonFormatTest {

    // Mirror of the prod <pattern> in logback-spring.xml (requestId + escaping).
    private static final String PROD_PATTERN =
            "{\"level\":\"%level\",\"requestId\":\"%mdc{requestId:-}\","
                    + "\"msg\":\"%replace(%message){'[\\\\\"]','_'}\"}";

    @Test
    void json_pattern_escapes_quotes_and_backslashes() throws Exception {
        LoggerContext ctx = new LoggerContext();
        PatternLayout layout = new PatternLayout();
        layout.setContext(ctx);
        layout.setPattern(PROD_PATTERN);
        layout.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContextRemoteView(ctx.getLoggerContextRemoteView());
        event.setLevel(Level.INFO);
        event.setMessage("order placed with \"quoted\" and back\\slash");
        event.setMDCPropertyMap(java.util.Map.of("requestId", "test-req-42"));

        String line = layout.doLayout(event).strip();

        // The whole point: the line is still valid JSON despite the nasty message.
        JsonNode node = new ObjectMapper().readTree(line);
        assertThat(node.get("level").asText()).isEqualTo("INFO");
        assertThat(node.get("requestId").asText()).isEqualTo("test-req-42");
        assertThat(node.get("msg").asText()).doesNotContain("\"").doesNotContain("\\");
        assertThat(node.get("msg").asText()).contains("order placed");
    }

    @Test
    void request_id_is_empty_when_absent() throws Exception {
        LoggerContext ctx = new LoggerContext();
        PatternLayout layout = new PatternLayout();
        layout.setContext(ctx);
        layout.setPattern(PROD_PATTERN);
        layout.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContextRemoteView(ctx.getLoggerContextRemoteView());
        event.setLevel(Level.INFO);
        event.setMessage("startup");
        event.setMDCPropertyMap(java.util.Map.of());

        // No requestId in the MDC -> empty string, and the line is still valid JSON.
        JsonNode node = new ObjectMapper().readTree(layout.doLayout(event).strip());
        assertThat(node.get("requestId").asText()).isEmpty();
    }
}
