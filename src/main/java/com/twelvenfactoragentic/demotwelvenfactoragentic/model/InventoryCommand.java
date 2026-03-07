package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

/**
 * Factor 4 — Tools devem receber dados estruturados
 */
public record InventoryCommand(
        ActionType action,
        String productId,
        int quantity
) {}