package com.corporate.catalog;

import com.corporate.entity.Product;
import com.corporate.dao.ProductRepository;
import com.corporate.web.RateLimitFilter;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2")
class BulkOrderIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository productRepo;
    @Autowired RateLimitFilter rateLimitFilter;

    MockMvc mvc;
    Product a;
    Product b;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        rateLimitFilter.reset();

        List<Product> inStock = productRepo.findAllByOrderByNameAsc().stream()
                .filter(p -> p.isInStock() && p.getStockQuantity() > 0)
                .limit(2)
                .toList();
        a = inStock.get(0);
        b = inStock.get(1);
    }

    private String estimate(String json) throws Exception {
        return mvc.perform(post("/api/bulk-order/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void prices_valid_slugs_and_returns_token_and_total() throws Exception {
        String json = """
                {"lines":[{"productSlug":"%s","quantity":2},{"productSlug":"%s","quantity":3}]}"""
                .formatted(a.getSlug(), b.getSlug());

        JsonNode res = mapper.readTree(estimate(json));

        assertThat(res.get("token").asText()).isNotBlank();
        assertThat(res.get("lines")).hasSize(2);
        long expected = a.getPriceCents() * 2 + b.getPriceCents() * 3;
        assertThat(res.get("totalCents").asLong()).isEqualTo(expected);
        assertThat(res.get("warnings")).isEmpty();
    }

    @Test
    void unknown_slug_is_dropped_with_a_warning() throws Exception {
        String json = """
                {"lines":[{"productSlug":"%s","quantity":1},{"productSlug":"no-such-product","quantity":1}]}"""
                .formatted(a.getSlug());

        JsonNode res = mapper.readTree(estimate(json));

        // Only the known, in-stock line is persisted; the unknown one is a warning.
        assertThat(res.get("lines")).hasSize(1);
        assertThat(res.get("lines").get(0).get("productSlug").asText()).isEqualTo(a.getSlug());
        assertThat(res.get("warnings").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void quantity_over_stock_is_excluded_from_total_with_a_warning() throws Exception {
        int tooMany = a.getStockQuantity() + 50;
        String json = """
                {"lines":[{"productSlug":"%s","quantity":%d}]}"""
                .formatted(a.getSlug(), tooMany);

        JsonNode res = mapper.readTree(estimate(json));

        // Not fulfillable -> not persisted, zero total, and flagged.
        assertThat(res.get("lines")).isEmpty();
        assertThat(res.get("totalCents").asLong()).isZero();
        assertThat(res.get("warnings").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void empty_lines_are_rejected() throws Exception {
        mvc.perform(post("/api/bulk-order/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversized_batch_is_rejected() throws Exception {
        String rows = IntStream.rangeClosed(1, 201)
                .mapToObj(i -> "{\"productSlug\":\"" + a.getSlug() + "\",\"quantity\":1}")
                .collect(Collectors.joining(","));
        mvc.perform(post("/api/bulk-order/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[" + rows + "]}"))
                .andExpect(status().isBadRequest());
    }
}
