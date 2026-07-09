package com.corporate.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.corporate.entity.Product;
import com.corporate.dao.ProductRepository;
import com.corporate.web.RateLimitFilter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2")
class CheckoutIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository productRepo;
    @Autowired RateLimitFilter rateLimitFilter;

    MockMvc mvc;
    String tokenA;
    String tokenB;
    Long productId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        rateLimitFilter.reset();

        tokenA = registerAndGetToken("alice-" + uniqueSuffix() + "@example.com");
        tokenB = registerAndGetToken("bob-" + uniqueSuffix() + "@example.com");

        Product p = productRepo.findAllByOrderByNameAsc().stream()
                .filter(x -> x.getStockQuantity() > 0)
                .findFirst()
                .orElseThrow();
        productId = p.getId();
    }

    @Test
    void places_an_order_and_decrements_stock() throws Exception {
        int startingStock = productRepo.findById(productId).orElseThrow().getStockQuantity();

        String body = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(productId, 2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode order = mapper.readTree(body);
        assertThat(order.get("orderNumber").asText()).startsWith("CG-");
        assertThat(order.get("status").asText()).isEqualTo("PLACED");

        int afterStock = productRepo.findById(productId).orElseThrow().getStockQuantity();
        assertThat(afterStock).isEqualTo(startingStock - 2);
    }

    @Test
    void idempotency_key_returns_same_order_on_replay() throws Exception {
        String key = "test-key-" + uniqueSuffix();

        String first = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(productId, 1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int stockAfterFirst = productRepo.findById(productId).orElseThrow().getStockQuantity();

        String replay = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(productId, 1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int stockAfterReplay = productRepo.findById(productId).orElseThrow().getStockQuantity();

        assertThat(mapper.readTree(first).get("orderNumber").asText())
                .isEqualTo(mapper.readTree(replay).get("orderNumber").asText());
        assertThat(stockAfterReplay).isEqualTo(stockAfterFirst);
    }

    @Test
    void rejects_when_stock_insufficient() throws Exception {
        int available = productRepo.findById(productId).orElseThrow().getStockQuantity();

        mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(productId, available + 1)))
                .andExpect(status().isConflict());
    }

    @Test
    void cross_user_cannot_fetch_order() throws Exception {
        String body = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(productId, 1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = mapper.readTree(body).get("orderNumber").asText();

        mvc.perform(get("/api/orders/" + orderNumber)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/orders/" + orderNumber)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void requires_authentication() throws Exception {
        mvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutJson(productId, 1)))
                .andExpect(status().isForbidden());
    }

    private String registerAndGetToken(String email) throws Exception {
        String req = """
                {"email":"%s","password":"hunter22!","fullName":"Test User","companyName":"Acme","phone":""}
                """.formatted(email);
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    private String checkoutJson(Long productId, int qty) {
        return """
                {
                  "customer":{"companyName":"Acme","contactName":"Jane","email":"j@acme.test","phone":""},
                  "shippingAddress":{"line1":"100 Market St","line2":null,"city":"SF","state":"CA","postalCode":"94105","country":"USA"},
                  "items":[{"productId":%d,"quantity":%d}]
                }
                """.formatted(productId, qty);
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime(), 36);
    }
}
