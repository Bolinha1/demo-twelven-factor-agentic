package com.twelvenfactoragentic.demotwelvenfactoragentic.resources;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Demonstrações dos recursos do Spring AI alinhados aos 12-Factor Agents.
 *
 * Não são testes de integração do agente — são exemplos executáveis que ilustram
 * como cada recurso do Spring AI funciona de forma isolada, sem LLM nem banco de dados.
 */
class TwelveFactorAgentsResourcesDemo {

    // =========================================================================
    // Factor 1 — Natural Language → Structured Calls
    // PromptTemplate: separação entre template e dados variáveis
    // =========================================================================

    @Nested
    class Factor1_PromptTemplate {

        /**
         * PromptTemplate permite definir o texto do prompt com variáveis
         * explícitas ({produto}, {quantidade}) e renderizá-las em tempo de execução.
         *
         * Benefício: o template é versionável, testável e independente do LLM.
         */
        @Test
        void renderiza_template_inline_com_variaveis_de_produto_e_quantidade() {
            PromptTemplate template = new PromptTemplate(
                    "Você deseja registrar a ENTRADA de {quantidade} unidades do produto '{produto}'."
            );

            String resultado = template.render(Map.of(
                    "produto", "camiseta",
                    "quantidade", 10
            ));

            assertThat(resultado)
                    .contains("ENTRADA")
                    .contains("camiseta")
                    .contains("10");
        }

        /**
         * Em produção, os templates ficam em arquivos .st no classpath.
         * PromptTemplate aceita um Resource — o mesmo padrão usado em
         * AgentReducer via @Value("classpath:prompts/...").
         */
        @Test
        void carrega_template_de_arquivo_classpath_e_renderiza() {
            PromptTemplate template = new PromptTemplate(
                    new ClassPathResource("prompts/confirmation.stock_in.prompt.st")
            );

            String resultado = template.render(Map.of(
                    "produto", "tênis",
                    "quantidade", 3
            ));

            assertThat(resultado)
                    .contains("tênis")
                    .contains("3");
        }
    }

    // =========================================================================
    // Factor 3 — Own Your Context Window
    // ChatMemory: controle explícito do histórico conversacional
    // =========================================================================

    @Nested
    class Factor3_ChatMemory {

        /**
         * Hierarquia dos componentes de memória:
         *
         *   ChatMemoryRepository  ← onde as mensagens são persistidas
         *         │
         *         ▼
         *   MessageWindowChatMemory  ← política de janela (quantas mensagens manter)
         *         │
         *         ▼
         *   MessageChatMemoryAdvisor  ← injeta/recupera histórico no ChatClient
         */
        @Test
        void constroi_memory_com_repositorio_in_memory_e_janela_de_10_mensagens() {
            /*
             * InMemoryChatMemoryRepository: para dev e testes.
             * Em produção é substituído por JdbcChatMemoryRepository
             * (auto-configurado via spring-ai-starter-model-chat-memory-repository-jdbc).
             */
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(10)
                    .build();

            assertThat(memory).isNotNull();
        }

        /**
         * O MessageChatMemoryAdvisor intercepta cada chamada ao ChatClient:
         * - Antes: busca histórico pelo conversationId e injeta no prompt
         * - Depois: persiste a nova mensagem no repositório
         *
         * O conversationId é passado em cada chamada:
         *   chatClient.prompt()
         *       .user(input)
         *       .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
         *       .call()
         */
        @Test
        void configura_chat_client_com_memory_advisor() {
            ChatMemory memory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(new InMemoryChatMemoryRepository())
                    .maxMessages(10)
                    .build();

            ChatModel mockChatModel = mock(ChatModel.class);

            ChatClient client = ChatClient.builder(mockChatModel)
                    .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
                    .build();

            assertThat(client).isNotNull();
        }

        /**
         * A troca do repositório (InMemory → Jdbc) não altera nenhuma outra camada.
         * É a única diferença entre o ambiente de dev e produção para a memória.
         *
         * Ver ChatMemoryConfig.java — configuração equivalente em produção:
         *
         *   @Bean
         *   ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
         *       return MessageWindowChatMemory.builder()
         *               .chatMemoryRepository(repository)
         *               .build();
         *   }
         */
        @Test
        void demonstra_troca_de_repositorio_sem_alterar_a_api_de_memoria() {
            var repoDesenvolvimento = new InMemoryChatMemoryRepository();
            // Produção: new JdbcChatMemoryRepository(jdbcTemplate)

            ChatMemory memory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(repoDesenvolvimento)
                    .build();

            assertThat(memory).isNotNull();
        }
    }
}
