package com.twelvenfactoragentic.demotwelvenfactoragentic.service;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.ActionType;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.AgentPhase;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.AgentState;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.InventoryCommand;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.ReducerResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Factor 12 — Make Your Agent a Stateless Reducer
 * Factor 8  — Own Your Control Flow (retry explícito)
 * Factor 9  — Compact Errors into Context (via ErrorContextService)
 *
 * Reducer puro: (AgentState, input) -> ReducerResult
 *
 * Toda logica de transicao de estado esta aqui.
 * InventoryAgentService apenas mantem o mapa de estados e delega ao reducer.
 */
@Component
public class AgentReducer {

    /**
     * Factor 8 — numero maximo de tentativas para extrair intencao do LLM
     */
    private static final int MAX_EXTRACTION_RETRIES = 2;

    private final ChatClient agentChatClient;
    private final ChatClient confirmationChatClient;
    private final InventoryService inventoryService;
    private final ErrorContextService errorContextService;

    public AgentReducer(
            @Qualifier("agentChatClient") ChatClient agentChatClient,
            @Qualifier("confirmationChatClient") ChatClient confirmationChatClient,
            InventoryService inventoryService,
            ErrorContextService errorContextService) {
        this.agentChatClient = agentChatClient;
        this.confirmationChatClient = confirmationChatClient;
        this.inventoryService = inventoryService;
        this.errorContextService = errorContextService;
    }

    /**
     * Funcao de reducao principal.
     * Recebe o estado atual e a entrada do usuario, retorna novo estado + resposta.
     */
    public ReducerResult reduce(AgentState state, String input) {
        if (state.phase() == AgentPhase.AWAITING_CONFIRMATION) {
            return handleConfirmation(state, input);
        }
        return handleNewIntent(state, input);
    }

    // --- Fase IDLE: extrai intencao com retry (Factor 8) ---

    private ReducerResult handleNewIntent(AgentState state, String input) {
        InventoryCommand command = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_EXTRACTION_RETRIES; attempt++) {
            try {
                String conversationId = Objects.requireNonNull(state.conversationId(),
                        "conversationId nao pode ser nulo");
                command = agentChatClient.prompt()
                        .user(input)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .call()
                        .entity(InventoryCommand.class);

                if (isValidCommand(command)) {
                    break;
                }
                command = null;
            } catch (Exception e) {
                lastException = e;
                command = null;
            }
        }

        if (command == null) {
            // Factor 9: compacta o erro antes de retornar
            String errorMsg = lastException != null
                    ? errorContextService.compact("extractIntent", lastException)
                    : "Nao foi possivel identificar uma operacao de estoque valida. " +
                      "Por favor, informe o produto, a quantidade e a operacao desejada.";
            return new ReducerResult(state, errorMsg);
        }

        AgentState newState = state.withPending(command);
        String confirmation = askConfirmation(command);
        return new ReducerResult(newState, confirmation);
    }

    // --- Fase AWAITING_CONFIRMATION: processa resposta do usuario (determinístico) ---

    private ReducerResult handleConfirmation(AgentState state, String input) {
        if (isAffirmative(input)) {
            String response = executeCommand(state.pendingCommand());
            return new ReducerResult(state.reset(), response);
        } else if (isNegative(input)) {
            return new ReducerResult(state.reset(),
                    "Operacao cancelada. Nenhuma alteracao foi realizada no estoque.");
        } else {
            String retry = "Nao entendi sua resposta. Responda 'sim' para confirmar ou 'nao' para cancelar.\n\n"
                    + askConfirmation(state.pendingCommand());
            return new ReducerResult(state, retry);
        }
    }

    // --- Execucao deterministica no servico de negocio, sem LLM (Factor 4 + 5) ---

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
            // Factor 9: erro compactado em contexto estruturado
            String operation = command.action() == ActionType.STOCK_IN ? "stockIn" : "stockOut";
            return errorContextService.compact(operation, e);
        }
    }

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

    private boolean isValidCommand(InventoryCommand command) {
        return command != null
                && command.productId() != null
                && command.action() != null
                && command.quantity() > 0;
    }

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
