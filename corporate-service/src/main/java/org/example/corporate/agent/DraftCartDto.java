package org.example.corporate.agent;

import java.util.List;

/**
 * Draft-cart view returned both by the create_draft_cart tool and by
 * GET /api/agent/draft-cart/{token}. One shape the frontend can adopt directly:
 * priced lines (paise), a grand total, and any warnings raised while pricing
 * (unknown slug, out-of-stock) so the buyer sees why a line was dropped.
 */
public record DraftCartDto(
        String token,
        List<Line> lines,
        long totalCents,
        List<String> warnings
) {
    public record Line(
            String productSlug,
            String productName,
            int quantity,
            long unitPriceCents,
            long lineTotalCents
    ) {}

    static DraftCartDto from(DraftCart cart, List<String> warnings) {
        List<Line> lines = cart.getItems().stream()
                .map(i -> new Line(
                        i.getProductSlug(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getUnitPriceCents(),
                        i.getLineTotalCents()))
                .toList();
        return new DraftCartDto(cart.getToken(), lines, cart.getTotalCents(),
                warnings == null ? List.of() : warnings);
    }
}
