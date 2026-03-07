package com.twelvenfactoragentic.demotwelvenfactoragentic.service;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.ActionType;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.InventoryCommand;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryAgentService {

    private final ChatClient agentChatClient;
    private final ChatClient confirmationChatClient;
    private final InventoryService inventoryService;

    /**
     * Factor 3 — Estado de confirmacao pendente controlado explicitamente na JVM,
     * fora do LLM. Cada conversationId mantem sua propria operacao pendente.
     */
    private final Map<String, InventoryCommand> pendingCommands = new ConcurrentHashMap<>();

    public InventoryAgentService(
            @Qualifier("agentChatClient") ChatClient agentChatClient,
            @Qualifier("confirmationChatClient") ChatClient confirmationChatClient,
            InventoryService inventoryService) {
        this.agentChatClient = agentChatClient;
        this.confirmationChatClient = confirmationChatClient;
        this.inventoryService = inventoryService;
    }

    /**
     * Factor 1 — Linguagem natural -> pipeline estruturado de dois estagios.
     *
     * Fluxo:
     * 1. Se ha comando pendente para este conversationId: trata como resposta de confirmacao.
     * 2. Se nao ha: extrai intencao estruturada -> guarda pendente -> retorna pergunta de confirmacao.
     */
    public String process(String message, String conversationId) {
        InventoryCommand pending = pendingCommands.get(conversationId);

        if (pending != null) {
            return handleConfirmation(message, conversationId, pending);
        }

        return handleNewIntent(message, conversationId);
    }

    // --- Fase 1: agentChatClient extrai a intencao como InventoryCommand ---

    private String handleNewIntent(String message, String conversationId) {
        InventoryCommand command = agentChatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(InventoryCommand.class);

        if (command == null || command.productId() == null || command.action() == null || command.quantity() <= 0) {
            return "Nao foi possivel identificar uma operacao de estoque valida. " +
                   "Por favor, informe o produto, a quantidade e a operacao desejada.";
        }

        pendingCommands.put(conversationId, command);

        return askConfirmation(command);
    }

    // --- Fase 2: confirmationChatClient formata a pergunta por tipo de operacao ---

    private String askConfirmation(InventoryCommand command) {
        String input = String.format(
                "Operacao: %s | Produto: %s | Quantidade: %d",
                command.action().name(),
                command.productId(),
                command.quantity()
        );

        return confirmationChatClient.prompt()
                .user(input)
                .call()
                .content();
    }

    // --- Tratamento da resposta de confirmacao ---

    private String handleConfirmation(String message, String conversationId, InventoryCommand pending) {
        if (isAffirmative(message)) {
            pendingCommands.remove(conversationId);
            return executeCommand(pending);
        } else if (isNegative(message)) {
            pendingCommands.remove(conversationId);
            return "Operacao cancelada. Nenhuma alteracao foi realizada no estoque.";
        } else {
            return "Nao entendi sua resposta. Responda 'sim' para confirmar ou 'nao' para cancelar.\n\n"
                    + askConfirmation(pending);
        }
    }

    // --- Factor 4 + Factor 5: execucao direta no servico de negocio, sem LLM ---

    private String executeCommand(InventoryCommand command) {
        try {
            if (command.action() == ActionType.STOCK_IN) {
                inventoryService.stockIn(command.productId(), command.quantity());
                return String.format("Entrada de %d unidades do produto '%s' registrada com sucesso.",
                        command.quantity(), command.productId());
            } else {
                inventoryService.stockOut(command.productId(), command.quantity());
                return String.format("Saida de %d unidades do produto '%s' registrada com sucesso.",
                        command.quantity(), command.productId());
            }
        } catch (Exception e) {
            return String.format("Erro ao executar a operacao: %s", e.getMessage());
        }
    }

    // --- Deteccao deterministica de confirmacao, sem round-trip ao LLM ---

    private boolean isAffirmative(String message) {
        if (message == null) return false;
        String normalized = message.trim().toLowerCase();
        return List.of("sim", "s", "pode", "confirmo", "confirmar", "ok", "yes", "y",
                        "claro", "com certeza", "positivo", "afirmativo", "vai", "ta bom")
                .stream().anyMatch(normalized::contains);
    }

    private boolean isNegative(String message) {
        if (message == null) return false;
        String normalized = message.trim().toLowerCase();
        return List.of("nao", "n", "cancela", "cancelar", "no", "nunca",
                        "negativo", "aborta", "abortar", "para")
                .stream().anyMatch(normalized::contains);
    }
}
