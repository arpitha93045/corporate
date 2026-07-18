package com.corporate.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.corporate.dao.ProductRepository;
import com.corporate.dao.UserRepository;
import com.corporate.entity.AppUser;
import com.corporate.entity.Product;
import com.corporate.entity.Role;
import com.corporate.web.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2")
class Net30InvoiceIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository productRepo;
    @Autowired UserRepository userRepo;
    @Autowired RateLimitFilter rateLimitFilter;

    MockMvc mvc;
    String buyerToken;
    String adminToken;
    Long productId;
    long productPriceCents;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        rateLimitFilter.reset();

        buyerToken = registerAndGetToken("buyer-" + uniqueSuffix() + "@example.com", false);
        adminToken = registerAndGetToken("admin-" + uniqueSuffix() + "@example.com", true);

        Product p = productRepo.findAllByOrderByNameAsc().stream()
                .filter(x -> x.getStockQuantity() > 0)
                .findFirst()
                .orElseThrow();
        productId = p.getId();
        productPriceCents = p.getPriceCents();
    }

    @Test
    void net30_checkout_places_invoice_order_and_decrements_stock() throws Exception {
        int startingStock = productRepo.findById(productId).orElseThrow().getStockQuantity();

        String body = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(net30Json(productId, 3, "PO-2026-0042")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode order = mapper.readTree(body);
        assertThat(order.get("status").asText()).isEqualTo("PLACED");
        assertThat(order.get("paymentTerms").asText()).isEqualTo("NET_30");
        assertThat(order.get("paymentStatus").asText()).isEqualTo("INVOICED");
        assertThat(order.get("poNumber").asText()).isEqualTo("PO-2026-0042");
        assertThat(order.get("invoiceNumber").asText()).startsWith("INV-");
        assertThat(order.get("dueDate").asText()).isEqualTo(LocalDate.now().plusDays(30).toString());
        // Server prices from the catalog.
        assertThat(order.get("subtotalCents").asLong()).isEqualTo(productPriceCents * 3);

        // Invoice terms don't skip stock reservation.
        int afterStock = productRepo.findById(productId).orElseThrow().getStockQuantity();
        assertThat(afterStock).isEqualTo(startingStock - 3);
    }

    @Test
    void net30_without_po_number_is_bad_request() throws Exception {
        String body = """
                {
                  "customer":{"companyName":"Acme","contactName":"Jane","email":"j@acme.test","phone":""},
                  "shippingAddress":{"line1":"100 Market St","line2":null,"city":"SF","state":"CA","postalCode":"94105","country":"USA"},
                  "items":[{"productId":%d,"quantity":1}],
                  "paymentTerms":"NET_30"
                }
                """.formatted(productId);

        mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_without_payment_terms_defaults_to_immediate() throws Exception {
        String body = """
                {
                  "customer":{"companyName":"Acme","contactName":"Jane","email":"j@acme.test","phone":""},
                  "shippingAddress":{"line1":"100 Market St","line2":null,"city":"SF","state":"CA","postalCode":"94105","country":"USA"},
                  "items":[{"productId":%d,"quantity":1}]
                }
                """.formatted(productId);

        String res = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode order = mapper.readTree(res);
        assertThat(order.get("paymentTerms").asText()).isEqualTo("IMMEDIATE");
        assertThat(order.get("invoiceNumber").isNull()).isTrue();
    }

    @Test
    void admin_marks_invoice_paid() throws Exception {
        String orderNumber = placeNet30Order("PO-1");

        String res = mvc.perform(post("/api/admin/orders/" + orderNumber + "/mark-invoice-paid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode order = mapper.readTree(res);
        assertThat(order.get("status").asText()).isEqualTo("PAID");
        assertThat(order.get("paymentStatus").asText()).isEqualTo("PAID");
        assertThat(order.get("paidAt").isNull()).isFalse();
    }

    @Test
    void marking_invoice_paid_twice_conflicts() throws Exception {
        String orderNumber = placeNet30Order("PO-2");

        mvc.perform(post("/api/admin/orders/" + orderNumber + "/mark-invoice-paid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/orders/" + orderNumber + "/mark-invoice-paid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void marking_a_card_order_invoice_paid_conflicts() throws Exception {
        // A default (IMMEDIATE) order isn't an invoice order.
        String res = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer":{"companyName":"Acme","contactName":"Jane","email":"j@acme.test","phone":""},
                                  "shippingAddress":{"line1":"100 Market St","line2":null,"city":"SF","state":"CA","postalCode":"94105","country":"USA"},
                                  "items":[{"productId":%d,"quantity":1}]
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orderNumber = mapper.readTree(res).get("orderNumber").asText();

        mvc.perform(post("/api/admin/orders/" + orderNumber + "/mark-invoice-paid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void marking_unknown_order_invoice_paid_is_not_found() throws Exception {
        mvc.perform(post("/api/admin/orders/CG-9999-999999/mark-invoice-paid")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

    private String placeNet30Order(String po) throws Exception {
        String res = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(net30Json(productId, 1, po)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(res).get("orderNumber").asText();
    }

    private String net30Json(Long productId, int qty, String po) {
        return """
                {
                  "customer":{"companyName":"Acme","contactName":"Jane","email":"j@acme.test","phone":""},
                  "shippingAddress":{"line1":"100 Market St","line2":null,"city":"SF","state":"CA","postalCode":"94105","country":"USA"},
                  "items":[{"productId":%d,"quantity":%d}],
                  "paymentTerms":"NET_30",
                  "poNumber":"%s"
                }
                """.formatted(productId, qty, po);
    }

    @Transactional
    String registerAndGetToken(String email, boolean admin) throws Exception {
        String req = """
                {"email":"%s","password":"hunter22!","fullName":"Test User","companyName":"Acme","phone":""}
                """.formatted(email);
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        if (!admin) {
            return mapper.readTree(body).get("token").asText();
        }

        AppUser user = userRepo.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepo.save(user);

        // Re-login so the JWT carries the ADMIN role claim.
        String login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"hunter22!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(login).get("token").asText();
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime(), 36);
    }
}
