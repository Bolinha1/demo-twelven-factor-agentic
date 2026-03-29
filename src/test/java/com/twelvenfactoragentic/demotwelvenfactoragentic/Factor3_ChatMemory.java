package com.twelvenfactoragentic.demotwelvenfactoragentic;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class Factor3_ChatMemory {

    @Autowired
    ChatModel chatModel;

    @Test
    void constroi_memory_com_repositorio_in_memory_e_janela_de_10_mensagens() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        assertThat(memory).isNotNull();
    }

    @Test
    void simula_conversa_com_historico_usando_memory_advisor() {
        String conversationId = "conversa-teste-001";

        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        String resposta1 = client.prompt()
                .user("Quero registrar a entrada de 20 unidades do produto 'camiseta'.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        assertThat(resposta1).isNotNull().isNotBlank();

        String resposta2 = client.prompt()
                .user("Quantas unidades eu disse que queria registrar, e para qual produto?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        assertThat(resposta2).isNotNull().isNotBlank();
        assertThat(resposta2).containsIgnoringCase("20");
        assertThat(resposta2).containsIgnoringCase("camiseta");
    }

    @Test
    void demonstra_troca_de_repositorio_sem_alterar_a_api_de_memoria() {
        var repoDesenvolvimento = new InMemoryChatMemoryRepository();

        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repoDesenvolvimento)
                .build();

        assertThat(memory).isNotNull();
    }
}