package com.corporate.service;

import com.corporate.entity.Product;
import com.corporate.dao.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.corporate.dao.ProductTagRepository;
import com.corporate.dto.AgentCartLine;
import com.corporate.dto.AgentCartTotal;
import com.corporate.dto.AgentProductRef;

/**
 * Backend implementations of the AI Gifting Agent's tools. Kept as ordinary
 * Java methods (not REST endpoints) — the agent controller in a later slice
 * will call these directly. That avoids a public tool API surface the model
 * could be tricked into hitting from the browser.
 *
 * Every method here is deterministic given the DB state: no LLM calls, no
 * network I/O, no clock. That makes them cheap to unit test and safe to log.
 *
 * Money in paise, prices come from the DB — never from the model.
 */
@Service
public class AgentTools {

    /** Hard cap on rows returned per search. Keeps prompt bloat bounded even
     *  when the model asks for a broad query. */
    static final int MAX_SEARCH_RESULTS = 12;

    /** Hard cap on quantity per line in estimate_total. Prevents a runaway
     *  suggestion from producing a 10^9-paise "estimate". */
    static final int MAX_QUANTITY_PER_LINE = 500;

    private final ProductRepository productRepo;
    private final ProductTagRepository tagRepo;

    public AgentTools(ProductRepository productRepo, ProductTagRepository tagRepo) {
        this.productRepo = productRepo;
        this.tagRepo = tagRepo;
    }

    /**
     * Full-text-ish search over name + description, optionally filtered by
     * a set of tags (all must match — intersection semantics). The DB does
     * the tag filter; the text filter is a case-insensitive contains match
     * over name + description in Java to keep the query H2-portable.
     *
     * @param query      free-text query; empty/null returns all in-scope products
     * @param tags       required tags, e.g. ["occasion:diwali", "dietary:vegetarian"]
     * @param maxResults soft cap (min 1); the method also enforces {@link #MAX_SEARCH_RESULTS}
     */
    @Transactional(readOnly = true)
    public List<AgentProductRef> searchProducts(String query, List<String> tags, int maxResults) {
        int cap = Math.min(Math.max(1, maxResults), MAX_SEARCH_RESULTS);

        List<Product> candidates;
        if (tags != null && !tags.isEmpty()) {
            List<Long> ids = tagRepo.findProductIdsWithAllTags(tags, tags.size());
            if (ids.isEmpty()) return List.of();
            candidates = productRepo.findAllById(ids);
        } else {
            candidates = productRepo.findAllByOrderByNameAsc();
        }

        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Product> matched = candidates.stream()
                .filter(Product::isInStock)
                .filter(p -> needle.isEmpty()
                        || p.getName().toLowerCase(Locale.ROOT).contains(needle)
                        || p.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .limit(cap)
                .toList();

        return matched.stream().map(this::toRef).toList();
    }

    /** Full detail for a single product by slug. Empty if unknown or deleted. */
    @Transactional(readOnly = true)
    public Optional<AgentProductRef> getProduct(String slug) {
        return productRepo.findBySlug(slug).map(this::toRef);
    }

    /**
     * Server-priced cart estimate. The model may propose slugs it hallucinated
     * or ask for out-of-stock quantities; both cases show up as warnings and
     * the offending line is excluded from the total.
     */
    @Transactional(readOnly = true)
    public AgentCartTotal estimateTotal(List<AgentCartLine> proposedLines) {
        if (proposedLines == null || proposedLines.isEmpty()) {
            return new AgentCartTotal(List.of(), 0, List.of());
        }

        List<AgentCartTotal.Line> resolved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        long total = 0;

        // Fetch every referenced product in one query, then walk the request
        // in its original order so the model can correlate its own indices.
        List<String> slugs = proposedLines.stream()
                .map(AgentCartLine::productSlug)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
        Map<String, Product> bySlug = productRepo.findAllByOrderByNameAsc().stream()
                .filter(p -> slugs.contains(p.getSlug()))
                .collect(Collectors.toMap(Product::getSlug, p -> p));

        for (AgentCartLine line : proposedLines) {
            if (line.productSlug() == null || line.productSlug().isBlank()) {
                warnings.add("Empty product slug in line; skipped.");
                continue;
            }
            Product p = bySlug.get(line.productSlug());
            if (p == null) {
                warnings.add("Unknown product '" + line.productSlug() + "'; skipped.");
                continue;
            }
            int qty = line.quantity();
            if (qty <= 0) {
                warnings.add(p.getSlug() + ": quantity must be positive; skipped.");
                continue;
            }
            if (qty > MAX_QUANTITY_PER_LINE) {
                warnings.add(p.getSlug() + ": requested " + qty
                        + " exceeds per-line cap of " + MAX_QUANTITY_PER_LINE
                        + "; capped.");
                qty = MAX_QUANTITY_PER_LINE;
            }
            int available = p.getStockQuantity();
            boolean inStock = p.isInStock() && available >= qty;
            if (!inStock) {
                warnings.add(p.getSlug() + ": only " + available + " in stock (requested " + qty + ").");
            }
            long lineTotal = p.getPriceCents() * qty;
            resolved.add(new AgentCartTotal.Line(
                    p.getSlug(),
                    p.getName(),
                    qty,
                    p.getPriceCents(),
                    lineTotal,
                    inStock,
                    available
            ));
            if (inStock) total += lineTotal;
        }

        return new AgentCartTotal(resolved, total, Collections.unmodifiableList(warnings));
    }

    private AgentProductRef toRef(Product p) {
        List<String> tags = tagRepo.findTagsByProductId(p.getId());
        return new AgentProductRef(
                p.getId(),
                p.getSlug(),
                p.getName(),
                p.getCategory().getSlug(),
                p.getPriceCents(),
                p.getStockQuantity(),
                p.isInStock(),
                p.getDescription(),
                tags
        );
    }
}
