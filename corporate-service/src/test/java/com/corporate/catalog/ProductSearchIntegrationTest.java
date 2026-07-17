package com.corporate.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("h2")
class ProductSearchIntegrationTest {

    @Autowired WebApplicationContext ctx;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    @Test
    void search_matches_on_name() throws Exception {
        mvc.perform(get("/api/products").param("q", "chocolate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug=='artisan-chocolate-box')]").exists());
    }

    @Test
    void search_composes_with_category_filter() throws Exception {
        // "chocolate" is a hamper/food product, not in the bags category.
        mvc.perform(get("/api/products").param("q", "chocolate").param("category", "bags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_returns_empty_for_no_match() throws Exception {
        mvc.perform(get("/api/products").param("q", "zzznomatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void wildcard_characters_are_escaped_not_interpreted() throws Exception {
        // A bare "%" must be treated as a literal, not a match-everything wildcard.
        mvc.perform(get("/api/products").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void blank_query_returns_full_catalog() throws Exception {
        mvc.perform(get("/api/products").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(28));
    }
}
