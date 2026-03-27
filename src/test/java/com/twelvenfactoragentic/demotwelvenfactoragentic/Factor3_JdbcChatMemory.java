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
public class Factor3_JdbcChatMemory {

    @Autowired
    ChatModel chatModel;

    @Autowired
    JdbcChatMemoryRepository jdbcChatMemoryRepository;

    @Test
    void constroi_memory_com_repositorio_jdbc_apontando_para_postgres() {
        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(10)
                .build();

        assertThat(memory).isNotNull();
    }

    @Test
    void persiste_mensagens_no_postgres_apos_chamada_ao_chat_client() {
        String conversationId = UUID.randomUUID().toString();

        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        client.prompt()
                .user("Diga apenas: memória persistida.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        List<Message> mensagens = jdbcChatMemoryRepository.findByConversationId(conversationId);

        assertThat(mensagens).isNotEmpty();
    }

    @Test
    void recupera_historico_do_postgres_em_chamadas_subsequentes() {
        String conversationId = UUID.randomUUID().toString();

        ChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .build();

        ChatClient client = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                .build();

        client.prompt()
                .user("Meu produto favorito é PRODUTO-XYZ.")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        String resposta = client.prompt()
                .user("Qual produto eu mencionei?")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        assertThat(resposta).containsIgnoringCase("PRODUTO-XYZ");
    }
}