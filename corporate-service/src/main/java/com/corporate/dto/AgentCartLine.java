package com.corporate.dto;

/** One line of a proposed cart, addressed by slug so the model can't fabricate ids. */
public record AgentCartLine(String productSlug, int quantity) {}
