package com.twelvenfactoragentic.demotwelvenfactoragentic.tools;

import com.twelvenfactoragentic.demotwelvenfactoragentic.service.InventoryService;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;

@Component
public class InventoryTools {

    private final InventoryService inventoryService;

    public InventoryTools(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Factor 4 — Tool real executando acao (padrao tool-calling via LLM).
     *
     * Nota: esta classe nao esta mais conectada a nenhum ChatClient.
     * A execucao passou a ser feita diretamente em InventoryAgentService.executeCommand(),
     * apos confirmacao explicita do usuario — eliminando o LLM do caminho critico de escrita.
     * Mantida como referencia pedagogica do padrao de tool-calling.
     */
    @Tool(description = "Registrar entrada de estoque")
    public String stockIn(String productId, int quantity){

        inventoryService.stockIn(productId,quantity);

        return "Entrada registrada";
    }

    @Tool(description = "Registrar saída de estoque")
    public String stockOut(String productId, int quantity){

        inventoryService.stockOut(productId,quantity);

        return "Saída registrada";
    }

}