package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

public record StockInCommand(String productId, int quantity) implements InventoryCommand {}
