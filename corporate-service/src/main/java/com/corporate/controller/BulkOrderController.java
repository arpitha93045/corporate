package com.corporate.controller;

import com.corporate.dto.AgentCartLine;
import com.corporate.dto.BulkOrderRequest;
import com.corporate.dto.DraftCartDto;
import com.corporate.service.DraftCartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bulk-order estimate: a corporate buyer pastes/uploads CSV rows of
 * {productSlug, quantity}; the server re-prices every line against the catalog
 * and returns a persisted, priced draft cart the frontend can adopt into the
 * cart and check out through the normal flow.
 *
 * Pricing + persistence are delegated to {@link DraftCartService} (which uses
 * AgentTools.estimateTotal), so this controller adds no money or stock logic —
 * the server stays the sole authority. Open to guests, like the catalog;
 * checkout still requires login downstream. Rate-limited per IP in RateLimitFilter.
 */
@RestController
@RequestMapping("/api/bulk-order")
public class BulkOrderController {

    private final DraftCartService draftCartService;

    public BulkOrderController(DraftCartService draftCartService) {
        this.draftCartService = draftCartService;
    }

    @PostMapping("/estimate")
    public DraftCartDto estimate(@Valid @RequestBody BulkOrderRequest req) {
        List<AgentCartLine> lines = req.lines().stream()
                .map(l -> new AgentCartLine(l.productSlug(), l.quantity()))
                .toList();
        return draftCartService.create(lines);
    }
}
