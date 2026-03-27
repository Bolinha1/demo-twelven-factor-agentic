package com.twelvenfactoragentic.demotwelvenfactoragentic;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class Factor5_UnifyExecutionState {

    @Autowired
    ChatModel chatModel;

    @Autowired
    JdbcChatMemoryRepository jdbcChatMemoryRepository;

    @Test
    void retoma_conversa_a_partir_do_historico_persistido() {

        String conversationId = UUID.randomUUID().toString();

        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        client.prompt()
                .user("Meu produto favorito é camiseta. Guarde essa informação.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        ChatMemory memoriaRecuperada = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();

        ChatClient clientRecuperado = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memoriaRecuperada).build())
                .build();

        String resposta = clientRecuperado.prompt()
                .user("Qual é o meu produto favorito?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        assertThat(resposta)
                .isNotNull()
                .containsIgnoringCase("camiseta");
    }

    @Test
    void demonstra_forking_e_recovery_no_dominio_de_estoque() {
        String originalThreadId = "estoque-principal-" + UUID.randomUUID();
        String forkThreadId = "simulacao-venda-" + UUID.randomUUID();

        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        client.prompt()
                .user("Meu produto principal é CAMISETA-DRYFIT e o estoque atual dele é exatamente 100 unidades. Guarde essa informação.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, originalThreadId))
                .call()
                .content();

        List<Message> historicoOriginal = jdbcChatMemoryRepository.findByConversationId(originalThreadId);
        assertThat(historicoOriginal).isNotEmpty();

        jdbcChatMemoryRepository.saveAll(forkThreadId, historicoOriginal);

        ChatClient clientFork = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        String respostaFork = clientFork.prompt()
                .user("Qual é o meu produto principal e quantas unidades existem no estoque?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, forkThreadId))
                .call()
                .content();

        assertThat(respostaFork).containsIgnoringCase("camiseta");
        assertThat(respostaFork).contains("100");

        String respostaOriginal = client.prompt()
                .user("Qual é o meu produto principal e quantas unidades existem no estoque?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, originalThreadId))
                .call()
                .content();

        assertThat(respostaOriginal).containsIgnoringCase("camiseta");
        assertThat(respostaOriginal).contains("100");
    }

    @Test
    void demonstra_forking_via_chat_memory_com_janelamento() {
        String threadOriginal = "original-memory-" + UUID.randomUUID();
        String threadFork = "fork-memory-" + UUID.randomUUID();

        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        client.prompt()
                .user("O estoque atual do Produto: Tênis Esportivo é de 50 unidades.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, threadOriginal))
                .call()
                .content();

        List<Message> historico = jdbcChatMemoryRepository.findByConversationId(threadOriginal);
        assertThat(historico).isNotEmpty();

        memory.add(threadFork, historico);

        ChatClient clientFork = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        String respostaFork = clientFork.prompt()
                .user("Registrar saída de 30 unidades. Quanto resta no estoque?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, threadFork))
                .call()
                .content();

        assertThat(respostaFork).contains("20");

        String statusOriginal = client.prompt()
                .user("Qual o saldo atual do estoque?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, threadOriginal))
                .call()
                .content();

        assertThat(statusOriginal).contains("50");
    }
}