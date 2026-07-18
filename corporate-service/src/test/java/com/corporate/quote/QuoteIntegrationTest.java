package com.corporate.quote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.corporate.dao.UserRepository;
import com.corporate.entity.AppUser;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2")
class QuoteIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;
    @Autowired RateLimitFilter rateLimitFilter;

    MockMvc mvc;
    String adminToken;
    long productId;
    long productPriceCents;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        rateLimitFilter.reset();
        adminToken = registerAdminAndGetToken("admin-" + uniqueSuffix() + "@example.com");

        JsonNode products = mapper.readTree(
                mvc.perform(get("/api/products"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        JsonNode p = products.get(0);
        productId = p.get("id").asLong();
        productPriceCents = p.get("priceCents").asLong();
    }

    @Test
    void admin_creates_server_priced_quote_and_enquiry_becomes_quoted() throws Exception {
        long enquiryId = submitEnquiry();

        String body = mvc.perform(post("/api/admin/enquiries/" + enquiryId + "/quote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"productId":%d,"quantity":10}],"notes":"bulk deal","validUntil":"2099-01-01"}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode quote = mapper.readTree(body);
        assertThat(quote.get("status").asText()).isEqualTo("SENT");
        assertThat(quote.get("token").asText()).isNotBlank();
        // Server prices from the catalog — total derives from DB price, not any client input.
        assertThat(quote.get("totalCents").asLong()).isEqualTo(productPriceCents * 10);
        assertThat(quote.get("lines").get(0).get("unitPriceCents").asLong()).isEqualTo(productPriceCents);

        // Enquiry moved to QUOTED.
        JsonNode enquiries = adminListEnquiries();
        JsonNode e = findEnquiry(enquiries, enquiryId);
        assertThat(e.get("status").asText()).isEqualTo("QUOTED");
    }

    @Test
    void buyer_fetches_quote_by_token_without_auth() throws Exception {
        String token = createQuote(submitEnquiry());

        mvc.perform(get("/api/quotes/" + token))
                .andExpect(status().isOk());
    }

    @Test
    void buyer_accepts_quote() throws Exception {
        long enquiryId = submitEnquiry();
        String token = createQuote(enquiryId);

        String body = mvc.perform(post("/api/quotes/" + token + "/accept"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(body).get("status").asText()).isEqualTo("ACCEPTED");

        JsonNode e = findEnquiry(adminListEnquiries(), enquiryId);
        assertThat(e.get("status").asText()).isEqualTo("ACCEPTED");
    }

    @Test
    void buyer_declines_quote() throws Exception {
        long enquiryId = submitEnquiry();
        String token = createQuote(enquiryId);

        String body = mvc.perform(post("/api/quotes/" + token + "/decline"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(body).get("status").asText()).isEqualTo("DECLINED");

        JsonNode e = findEnquiry(adminListEnquiries(), enquiryId);
        assertThat(e.get("status").asText()).isEqualTo("DECLINED");
    }

    @Test
    void accepting_an_already_actioned_quote_conflicts() throws Exception {
        String token = createQuote(submitEnquiry());

        mvc.perform(post("/api/quotes/" + token + "/decline")).andExpect(status().isOk());
        mvc.perform(post("/api/quotes/" + token + "/accept")).andExpect(status().isConflict());
    }

    @Test
    void unknown_token_is_not_found() throws Exception {
        mvc.perform(get("/api/quotes/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void quote_for_missing_enquiry_is_not_found() throws Exception {
        mvc.perform(post("/api/admin/enquiries/999999/quote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"productId":%d,"quantity":1}]}
                                """.formatted(productId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void quote_with_missing_product_is_not_found() throws Exception {
        mvc.perform(post("/api/admin/enquiries/" + submitEnquiry() + "/quote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"productId":999999,"quantity":1}]}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void quote_with_empty_lines_is_bad_request() throws Exception {
        mvc.perform(post("/api/admin/enquiries/" + submitEnquiry() + "/quote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quote_with_zero_quantity_is_bad_request() throws Exception {
        mvc.perform(post("/api/admin/enquiries/" + submitEnquiry() + "/quote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"productId":%d,"quantity":0}]}
                                """.formatted(productId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_can_set_reviewing_status() throws Exception {
        long enquiryId = submitEnquiry();
        mvc.perform(patch("/api/admin/enquiries/" + enquiryId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVIEWING\"}"))
                .andExpect(status().isOk());
    }

    // ---- helpers ----

    private String createQuote(long enquiryId) throws Exception {
        String body = mvc.perform(post("/api/admin/enquiries/" + enquiryId + "/quote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"productId":%d,"quantity":5}]}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    private long submitEnquiry() throws Exception {
        mvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Buyer","email":"buyer-%s@acme.test","message":"200 gifts for Diwali","estimatedQuantity":200,"occasion":"Diwali"}
                                """.formatted(uniqueSuffix())))
                .andExpect(status().isCreated());
        JsonNode enquiries = adminListEnquiries();
        // Newest first — the one we just created.
        return enquiries.get(0).get("id").asLong();
    }

    private JsonNode adminListEnquiries() throws Exception {
        return mapper.readTree(mvc.perform(get("/api/admin/enquiries")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode findEnquiry(JsonNode list, long id) {
        for (JsonNode e : list) {
            if (e.get("id").asLong() == id) return e;
        }
        throw new AssertionError("enquiry " + id + " not in list");
    }

    @Transactional
    String registerAdminAndGetToken(String email) throws Exception {
        String req = """
                {"email":"%s","password":"hunter22!","fullName":"Admin","companyName":"Acme","phone":""}
                """.formatted(email);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isOk());

        AppUser user = userRepo.findByEmail(email).orElseThrow();
        user.setRole(Role.ADMIN);
        userRepo.save(user);

        // Re-login so the JWT carries the ADMIN role claim.
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"hunter22!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }

    private String uniqueSuffix() {
        return Long.toString(System.nanoTime(), 36);
    }
}
