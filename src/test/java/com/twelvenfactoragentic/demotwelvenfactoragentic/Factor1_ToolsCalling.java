package com.twelvenfactoragentic.demotwelvenfactoragentic;

import com.twelvenfactoragentic.demotwelvenfactoragentic.repository.ProductRepository;
import com.twelvenfactoragentic.demotwelvenfactoragentic.service.InventoryService;
import com.twelvenfactoragentic.demotwelvenfactoragentic.tools.InventoryTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class Factor1_ToolsCalling {

    @Autowired
    ChatModel chatModel;

    @Autowired
    InventoryTools inventoryTools;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    ProductRepository productRepository;

    @Test
    void llm_chama_tool_stock_in_via_linguagem_natural() {
        productRepository.deleteById("tenis");

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        chatClient.prompt()
                .user("Registre a entrada de 5 unidades do produto 'tenis'")
                .tools(inventoryTools)
                .call()
                .content();

        int quantidade = productRepository.findById("tenis")
                .orElseThrow()
                .getQuantity();

        assertThat(quantidade).isEqualTo(5);
    }

    @Test
    void llm_chama_tool_stock_out_via_linguagem_natural() {
        productRepository.deleteById("camiseta");
        inventoryService.stockIn("camiseta", 10);

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        chatClient.prompt()
                .user("Registre a saída de 3 unidades do produto 'camiseta'")
                .tools(inventoryTools)
                .call()
                .content();

        int quantidade = productRepository.findById("camiseta")
                .orElseThrow()
                .getQuantity();

        assertThat(quantidade).isEqualTo(7);
    }
}