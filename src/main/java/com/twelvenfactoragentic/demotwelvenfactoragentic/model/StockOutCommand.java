package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

public record StockOutCommand(String productId, int quantity) implements InventoryCommand {}
