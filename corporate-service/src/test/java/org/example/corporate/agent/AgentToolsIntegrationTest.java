package org.example.corporate.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class AgentToolsIntegrationTest {

    @Autowired AgentTools tools;

    @Test
    void searchByTextFindsBySlugFragmentInDescription() {
        List<AgentProductRef> hits = tools.searchProducts("laptop", List.of(), 10);

        assertThat(hits).extracting(AgentProductRef::slug)
                .contains("canvas-laptop-backpack", "executive-laptop-bag");
    }

    @Test
    void searchByTagsRequiresAllTags() {
        // Both tags must match. Diwali + vegetarian -> the sweets/premium hampers,
        // not the aromatherapy candle (which is vegan-tagged, not vegetarian).
        List<AgentProductRef> hits = tools.searchProducts(
                "", List.of("occasion:diwali", "dietary:vegetarian"), 10);

        assertThat(hits).extracting(AgentProductRef::slug)
                .contains("diwali-sweets-hamper")
                .doesNotContain("aromatherapy-candle-trio");
    }

    @Test
    void searchTextAndTagsCombine() {
        // 'sampler' text + festival tag -> the tea sampler is a festival gift
        // whose name+description uniquely contains 'sampler'.
        List<AgentProductRef> hits = tools.searchProducts(
                "sampler", List.of("occasion:festival"), 10);

        assertThat(hits).extracting(AgentProductRef::slug)
                .containsExactly("artisanal-tea-sampler");
    }

    @Test
    void searchCapsResultCount() {
        // Ask for way more than the internal cap; result count must respect it.
        List<AgentProductRef> hits = tools.searchProducts("", List.of(), 999);

        assertThat(hits).hasSizeLessThanOrEqualTo(AgentTools.MAX_SEARCH_RESULTS);
    }

    @Test
    void searchWithUnknownTagReturnsEmpty() {
        List<AgentProductRef> hits = tools.searchProducts(
                "", List.of("occasion:no-such-thing"), 10);
        assertThat(hits).isEmpty();
    }

    @Test
    void getProductReturnsRefWithTags() {
        Optional<AgentProductRef> ref = tools.getProduct("diwali-sweets-hamper");

        assertThat(ref).isPresent();
        assertThat(ref.get().tags())
                .contains("occasion:diwali", "dietary:vegetarian", "band:1500-3500");
    }

    @Test
    void getProductUnknownSlug() {
        assertThat(tools.getProduct("does-not-exist")).isEmpty();
    }

    @Test
    void estimateTotalPricesFromServer() {
        AgentCartTotal total = tools.estimateTotal(List.of(
                new AgentCartLine("artisanal-tea-sampler", 3)
        ));

        assertThat(total.lines()).hasSize(1);
        AgentCartTotal.Line line = total.lines().get(0);
        assertThat(line.quantity()).isEqualTo(3);
        assertThat(line.lineTotalCents()).isEqualTo(line.unitPriceCents() * 3);
        assertThat(total.totalCents()).isEqualTo(line.lineTotalCents());
        assertThat(total.warnings()).isEmpty();
    }

    @Test
    void estimateTotalWarnsAndSkipsUnknownSlug() {
        AgentCartTotal total = tools.estimateTotal(List.of(
                new AgentCartLine("ghost-product", 2),
                new AgentCartLine("artisanal-tea-sampler", 1)
        ));

        assertThat(total.lines()).extracting(AgentCartTotal.Line::productSlug)
                .containsExactly("artisanal-tea-sampler");
        assertThat(total.warnings())
                .anyMatch(w -> w.contains("ghost-product"));
        assertThat(total.totalCents()).isEqualTo(total.lines().get(0).lineTotalCents());
    }

    @Test
    void estimateTotalRejectsNonPositiveQuantity() {
        AgentCartTotal total = tools.estimateTotal(List.of(
                new AgentCartLine("artisanal-tea-sampler", 0),
                new AgentCartLine("artisanal-tea-sampler", -5)
        ));

        assertThat(total.lines()).isEmpty();
        assertThat(total.totalCents()).isZero();
        assertThat(total.warnings()).hasSize(2);
    }

    @Test
    void estimateTotalCapsPerLineQuantity() {
        AgentCartTotal total = tools.estimateTotal(List.of(
                new AgentCartLine("artisanal-tea-sampler", AgentTools.MAX_QUANTITY_PER_LINE + 100)
        ));

        assertThat(total.lines()).hasSize(1);
        assertThat(total.lines().get(0).quantity()).isEqualTo(AgentTools.MAX_QUANTITY_PER_LINE);
        assertThat(total.warnings()).anyMatch(w -> w.contains("exceeds per-line cap"));
    }
}
