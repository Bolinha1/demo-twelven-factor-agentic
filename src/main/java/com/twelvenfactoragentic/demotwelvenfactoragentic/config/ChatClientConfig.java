package com.twelvenfactoragentic.demotwelvenfactoragentic.config;

import com.twelvenfactoragentic.demotwelvenfactoragentic.tools.HumanConfirmationTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class ChatClientConfig {

    @Value("classpath:prompts/system.prompt.st")
    private Resource systemPromptResource;

    @Value("classpath:prompts/confirmation.prompt.st")
    private Resource confirmationPromptResource;

    /**
     * Factor 2 — Agente de extração de intenção
     * Factor 3 — Memória JDBC preserva o contexto conversacional entre turnos
     * Factor 7 — HumanConfirmationTool registrada: escalação humana via tool call explícita
     *
     * system.prompt.st é a única fonte de verdade para interpretação de linguagem natural.
     */
    @Bean
    @Qualifier("agentChatClient")
    ChatClient agentChatClient(ChatClient.Builder builder, ChatMemory chatMemory,
                               HumanConfirmationTool humanConfirmationTool) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        return builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(humanConfirmationTool)
                .build();
    }

    /**
     * Factor 2 — Agente de confirmação
     *
     * confirmation.prompt.st é a única fonte de verdade para a formatação da pergunta de confirmação.
     * Sem ferramentas e sem memória: recebe dados já estruturados, não precisa de histórico conversacional.
     */
    @Bean
    @Qualifier("confirmationChatClient")
    ChatClient confirmationChatClient(ChatClient.Builder builder) throws IOException {
        String confirmationPrompt = confirmationPromptResource.getContentAsString(StandardCharsets.UTF_8);
        return builder
                .defaultSystem(confirmationPrompt)
                .build();
    }
}
