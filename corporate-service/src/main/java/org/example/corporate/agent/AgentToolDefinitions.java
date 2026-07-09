package org.example.corporate.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The bridge between Claude tool-use and the deterministic slice-A tool methods
 * in {@link AgentTools}. Holds the JSON tool schemas advertised to the model and
 * a dispatcher that turns a {@code tool_use} block into a Java call and back into
 * a serializable result for the {@code tool_result}.
 *
 * Scope for this slice: search_products, get_product, estimate_total. The
 * create_draft_cart / create_enquiry tools come in a later slice.
 */
@Component
public class AgentToolDefinitions {

    private final AgentTools tools;
    private final ObjectMapper mapper;

    public AgentToolDefinitions(AgentTools tools, ObjectMapper mapper) {
        this.tools = tools;
        this.mapper = mapper;
    }

    /** Anthropic {@code tools} array to attach to every Messages request. */
    public ArrayNode toolSchemas() {
        ArrayNode arr = mapper.createArrayNode();

        // search_products
        ObjectNode search = arr.addObject();
        search.put("name", "search_products");
        search.put("description",
                "Search the gift catalog by free text and/or required tags. Returns in-stock "
                + "products with slug, name, price (in paise), stock, description, and tags. "
                + "Tags use a kind:value scheme, e.g. occasion:diwali, dietary:vegetarian, "
                + "audience:clients, band:premium. All supplied tags must match (intersection).");
        ObjectNode searchSchema = search.putObject("input_schema");
        searchSchema.put("type", "object");
        ObjectNode searchProps = searchSchema.putObject("properties");
        searchProps.putObject("query").put("type", "string")
                .put("description", "Free-text query over product name and description. May be empty.");
        ObjectNode searchTags = searchProps.putObject("tags");
        searchTags.put("type", "array").put("description",
                "Required tags, e.g. [\"occasion:diwali\",\"dietary:vegetarian\"]. May be empty.");
        searchTags.putObject("items").put("type", "string");
        searchProps.putObject("max_results").put("type", "integer")
                .put("description", "Soft cap on results (server also enforces a hard cap of 12).");
        searchSchema.putArray("required"); // all optional

        // get_product
        ObjectNode get = arr.addObject();
        get.put("name", "get_product");
        get.put("description", "Fetch full detail for a single product by its slug.");
        ObjectNode getSchema = get.putObject("input_schema");
        getSchema.put("type", "object");
        getSchema.putObject("properties").putObject("slug").put("type", "string")
                .put("description", "The product slug.");
        getSchema.putArray("required").add("slug");

        // estimate_total
        ObjectNode est = arr.addObject();
        est.put("name", "estimate_total");
        est.put("description",
                "Server-priced estimate for a proposed cart. YOU MUST use this for any total — "
                + "never compute prices yourself. Returns per-line and grand totals in paise, plus "
                + "warnings for unknown slugs or insufficient stock.");
        ObjectNode estSchema = est.putObject("input_schema");
        estSchema.put("type", "object");
        ObjectNode estProps = estSchema.putObject("properties");
        ObjectNode lines = estProps.putObject("lines");
        lines.put("type", "array").put("description", "Proposed cart lines.");
        ObjectNode lineItems = lines.putObject("items");
        lineItems.put("type", "object");
        ObjectNode lineProps = lineItems.putObject("properties");
        lineProps.putObject("product_slug").put("type", "string");
        lineProps.putObject("quantity").put("type", "integer");
        lineItems.putArray("required").add("product_slug").add("quantity");
        estSchema.putArray("required").add("lines");

        return arr;
    }

    /**
     * Dispatches a tool call to the matching {@link AgentTools} method and returns
     * a JSON-serializable result. Never throws for model-caused issues (unknown
     * slugs, out-of-stock) — those surface as warnings inside the result so the
     * model can react.
     *
     * @throws IllegalArgumentException only for a genuinely unknown tool name
     */
    public Object invoke(String toolName, JsonNode input) {
        return switch (toolName) {
            case "search_products" -> {
                String query = text(input, "query");
                List<String> tags = stringArray(input, "tags");
                int maxResults = input.hasNonNull("max_results") ? input.get("max_results").asInt() : 12;
                yield tools.searchProducts(query, tags, maxResults);
            }
            case "get_product" -> tools.getProduct(text(input, "slug"))
                    .<Object>map(ref -> ref)
                    .orElse(java.util.Map.of("error", "product not found"));
            case "estimate_total" -> {
                List<AgentCartLine> proposed = new ArrayList<>();
                JsonNode lines = input.get("lines");
                if (lines != null && lines.isArray()) {
                    for (JsonNode l : lines) {
                        proposed.add(new AgentCartLine(text(l, "product_slug"),
                                l.hasNonNull("quantity") ? l.get("quantity").asInt() : 0));
                    }
                }
                yield tools.estimateTotal(proposed);
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static List<String> stringArray(JsonNode node, String field) {
        List<String> out = new ArrayList<>();
        JsonNode arr = node == null ? null : node.get(field);
        if (arr != null && arr.isArray()) {
            arr.forEach(e -> out.add(e.asText()));
        }
        return out;
    }
}
