package org.example.corporate.agent;

import org.example.corporate.catalog.Product;
import org.example.corporate.catalog.ProductRepository;
import org.example.corporate.web.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Creates and fetches persisted draft carts for the agent. Pricing is delegated
 * to {@link AgentTools#estimateTotal} so the server, never the model, decides
 * money and stock. Only in-stock, resolvable lines are persisted; everything the
 * estimate flagged surfaces as a warning on the returned DTO.
 */
@Service
public class DraftCartService {

    private static final Logger log = LoggerFactory.getLogger(DraftCartService.class);

    private final DraftCartRepository repo;
    private final AgentTools agentTools;
    private final ProductRepository productRepo;

    public DraftCartService(DraftCartRepository repo, AgentTools agentTools, ProductRepository productRepo) {
        this.repo = repo;
        this.agentTools = agentTools;
        this.productRepo = productRepo;
    }

    @Transactional
    public DraftCartDto create(List<AgentCartLine> proposed) {
        AgentCartTotal priced = agentTools.estimateTotal(proposed);

        DraftCart cart = new DraftCart();
        cart.setToken(UUID.randomUUID().toString());
        long total = 0;

        for (AgentCartTotal.Line line : priced.lines()) {
            if (!line.inStock()) {
                // Never persist a line we can't currently fulfil; it's already in warnings.
                continue;
            }
            Product p = productRepo.findBySlug(line.productSlug()).orElse(null);
            if (p == null) {
                continue; // priced from a snapshot but now gone — skip defensively
            }
            DraftCartItem item = new DraftCartItem();
            item.setProductId(p.getId());
            item.setProductSlug(line.productSlug());
            item.setProductName(line.productName());
            item.setUnitPriceCents(line.unitPriceCents());
            item.setQuantity(line.quantity());
            item.setLineTotalCents(line.lineTotalCents());
            cart.addItem(item);
            total += line.lineTotalCents();
        }

        cart.setTotalCents(total);
        DraftCart saved = repo.save(cart);
        log.info("agent.draft_cart_created token={} lines={} total_cents={}",
                saved.getToken(), saved.getItems().size(), total);
        return DraftCartDto.from(saved, priced.warnings());
    }

    @Transactional(readOnly = true)
    public DraftCartDto fetch(String token) {
        DraftCart cart = repo.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Draft cart not found"));
        return DraftCartDto.from(cart, List.of());
    }
}
