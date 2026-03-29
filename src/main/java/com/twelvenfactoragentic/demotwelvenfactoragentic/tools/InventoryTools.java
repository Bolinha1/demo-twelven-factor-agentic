package com.twelvenfactoragentic.demotwelvenfactoragentic.tools;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.Product;
import com.twelvenfactoragentic.demotwelvenfactoragentic.service.InventoryService;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

@Component
public class InventoryTools {

    private final InventoryService inventoryService;

    public InventoryTools(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    
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

    @Tool(description = "Listar todos os produtos do estoque")
    public List<Product> listProducts(){
        return inventoryService.listProducts();
    }

}