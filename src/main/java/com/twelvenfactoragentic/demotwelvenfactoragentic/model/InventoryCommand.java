package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Factor 4 — Tools devem receber dados estruturados.
 * Sealed interface garante que o schema JSON gerado pelo Spring AI
 * use oneOf — cada subtipo carrega apenas os campos que fazem sentido,
 * eliminando campos opcionais ambíguos que induzem o modelo a errar.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StockInCommand.class,  name = "STOCK_IN"),
        @JsonSubTypes.Type(value = StockOutCommand.class, name = "STOCK_OUT"),
        @JsonSubTypes.Type(value = StockGetCommand.class, name = "STOCK_GET"),
        @JsonSubTypes.Type(value = UnknownCommand.class,  name = "UNKNOWN")
})
public sealed interface InventoryCommand
        permits StockInCommand, StockOutCommand, StockGetCommand, UnknownCommand {}
