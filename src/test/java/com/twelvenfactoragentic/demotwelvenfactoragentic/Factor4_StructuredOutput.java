package com.twelvenfactoragentic.demotwelvenfactoragentic;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.ActionType;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.InventoryCommand;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class Factor4_StructuredOutput {

    @Autowired
    ChatModel chatModel;

    @Test
    void llm_extrai_comando_stock_in_como_java_record() throws IOException {
        String systemPrompt = new ClassPathResource("prompts/system.prompt.st")
                .getContentAsString(StandardCharsets.UTF_8);

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        InventoryCommand command = chatClient.prompt()
                .system(systemPrompt)
                .user("Registre a entrada de 10 unidades do produto 'camiseta'")
                .call()
                .entity(InventoryCommand.class);

        assertThat(command)
                .isNotNull()
                .satisfies(cmd -> {
                    assertThat(cmd.action()).isEqualTo(ActionType.STOCK_IN);
                    assertThat(cmd.productId()).isEqualTo("camiseta");
                    assertThat(cmd.quantity()).isEqualTo(10);
                });
    }

    @Test
    void llm_extrai_comando_stock_out_como_java_record() throws IOException {
        String systemPrompt = new ClassPathResource("prompts/system.prompt.st")
                .getContentAsString(StandardCharsets.UTF_8);

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        InventoryCommand command = chatClient.prompt()
                .system(systemPrompt)
                .user("Quero retirar 3 unidades do produto 'tenis'")
                .call()
                .entity(InventoryCommand.class);

        assertThat(command)
                .isNotNull()
                .satisfies(cmd -> {
                    assertThat(cmd.action()).isEqualTo(ActionType.STOCK_OUT);
                    assertThat(cmd.productId()).isEqualTo("tenis");
                    assertThat(cmd.quantity()).isEqualTo(3);
                });
    }

    @Test
    void structured_output_e_a_base_do_tool_calling() throws IOException {
        String systemPrompt = new ClassPathResource("prompts/system.prompt.st")
                .getContentAsString(StandardCharsets.UTF_8);

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        InventoryCommand command = chatClient.prompt()
                .system(systemPrompt)
                .user("Preciso dar saída de 5 unidades do produto 'calca'")
                .call()
                .entity(InventoryCommand.class);

        assertThat(command)
                .isNotNull()
                .satisfies(cmd -> {
                    assertThat(cmd.action()).isNotNull();
                    assertThat(cmd.productId()).isNotBlank();
                    assertThat(cmd.quantity()).isGreaterThan(0);
                });
    }
}