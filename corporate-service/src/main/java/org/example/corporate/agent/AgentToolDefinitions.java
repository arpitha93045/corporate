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
    private final DraftCartService draftCartService;
    private final org.example.corporate.enquiry.EnquiryService enquiryService;
    private final ObjectMapper mapper;

    public AgentToolDefinitions(AgentTools tools,
                                DraftCartService draftCartService,
                                org.example.corporate.enquiry.EnquiryService enquiryService,
                                ObjectMapper mapper) {
        this.tools = tools;
        this.draftCartService = draftCartService;
        this.enquiryService = enquiryService;
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

        // create_draft_cart
        ObjectNode draft = arr.addObject();
        draft.put("name", "create_draft_cart");
        draft.put("description",
                "Persist your final proposed selection as a draft cart and get back a token the buyer "
                + "adopts with one click. Server-priced (unknown/out-of-stock lines are dropped and "
                + "reported in warnings). Propose-only: this does NOT place an order — the buyer still "
                + "completes checkout. Call this once you have a selection the buyer has accepted.");
        ObjectNode draftSchema = draft.putObject("input_schema");
        draftSchema.put("type", "object");
        ObjectNode draftProps = draftSchema.putObject("properties");
        ObjectNode draftLines = draftProps.putObject("lines");
        draftLines.put("type", "array").put("description", "The proposed cart lines.");
        ObjectNode draftLineItems = draftLines.putObject("items");
        draftLineItems.put("type", "object");
        ObjectNode draftLineProps = draftLineItems.putObject("properties");
        draftLineProps.putObject("product_slug").put("type", "string");
        draftLineProps.putObject("quantity").put("type", "integer");
        draftLineItems.putArray("required").add("product_slug").add("quantity");
        draftSchema.putArray("required").add("lines");

        // create_enquiry
        ObjectNode enq = arr.addObject();
        enq.put("name", "create_enquiry");
        enq.put("description",
                "Escalate to a human sales rep. Use ONLY when the request is too large, custom, or "
                + "complex to fulfil from the catalog (e.g. bulk branding, custom hampers, net-30 "
                + "invoicing, or the buyer explicitly asks to talk to someone). Requires the buyer's "
                + "name, email, and a message; ask for these before calling if you don't have them.");
        ObjectNode enqSchema = enq.putObject("input_schema");
        enqSchema.put("type", "object");
        ObjectNode enqProps = enqSchema.putObject("properties");
        enqProps.putObject("name").put("type", "string");
        enqProps.putObject("email").put("type", "string");
        enqProps.putObject("message").put("type", "string");
        enqProps.putObject("company_name").put("type", "string");
        enqProps.putObject("phone").put("type", "string");
        enqProps.putObject("estimated_quantity").put("type", "integer");
        enqProps.putObject("occasion").put("type", "string");
        enqProps.putObject("budget_range").put("type", "string");
        enqSchema.putArray("required").add("name").add("email").add("message");

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
            case "create_draft_cart" -> {
                List<AgentCartLine> proposed = new ArrayList<>();
                JsonNode lines = input.get("lines");
                if (lines != null && lines.isArray()) {
                    for (JsonNode l : lines) {
                        proposed.add(new AgentCartLine(text(l, "product_slug"),
                                l.hasNonNull("quantity") ? l.get("quantity").asInt() : 0));
                    }
                }
                yield draftCartService.create(proposed);
            }
            case "create_enquiry" -> {
                String name = text(input, "name");
                String email = text(input, "email");
                String message = text(input, "message");
                // Guard: EnquiryService.submit is not behind @Valid here, so enforce the
                // required fields ourselves and hand the model a clear error instead of
                // persisting junk. Real customer records — keep them clean.
                if (isBlank(name) || isBlank(email) || isBlank(message)) {
                    yield java.util.Map.of("error",
                            "name, email and message are all required to raise an enquiry");
                }
                if (!email.contains("@") || email.length() > 200) {
                    yield java.util.Map.of("error", "a valid email is required");
                }
                org.example.corporate.enquiry.EnquiryRequest req =
                        new org.example.corporate.enquiry.EnquiryRequest(
                                name,
                                email,
                                text(input, "company_name"),
                                text(input, "phone"),
                                message,
                                input.hasNonNull("estimated_quantity")
                                        ? input.get("estimated_quantity").asInt() : null,
                                text(input, "occasion"),
                                null,
                                text(input, "budget_range"));
                yield enquiryService.submit(req);
            }
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
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
