package com.twelvenfactoragentic.demotwelvenfactoragentic;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class Factor2_PromptTemplate {

    @Autowired
    ChatModel chatModel;

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

    @Test
    void envia_string_simples_sem_variaveis_ao_chat_client() {
        String prompt = "Você deseja registrar a ENTRADA de 10 unidades do produto 'camiseta'.";

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String resposta = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        assertThat(resposta).isNotNull().isNotBlank();
    }

    @Test
    void executa_prompt_renderizado_via_chat_client() {
        PromptTemplate template = new PromptTemplate(
                "Você deseja registrar a ENTRADA de {quantidade} unidades do produto '{produto}'."
        );
        String promptRenderizado = template.render(Map.of(
                "produto", "camiseta",
                "quantidade", 10
        ));

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String resposta = chatClient.prompt()
                .user(promptRenderizado)
                .call()
                .content();

        assertThat(resposta).isNotNull().isNotBlank();
    }

    @Test
    void envia_system_prompt_e_user_prompt_ao_chat_client() {
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        String resposta = chatClient.prompt()
                .system("Você é um assistente de controle de estoque. Responda sempre de forma objetiva e confirme a operação solicitada.")
                .user("Registre a entrada de 8 unidades do produto 'calça'.")
                .call()
                .content();

        assertThat(resposta).isNotNull().isNotBlank();
    }
}
