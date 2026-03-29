package com.twelvenfactoragentic.demotwelvenfactoragentic.service;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.InventoryCommand;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.StockGetCommand;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.StockInCommand;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.StockOutCommand;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.UnknownCommand;
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

    private final Map<String, InventoryCommand> pendingCommands = new ConcurrentHashMap<>();

    public InventoryAgentService(
            @Qualifier("agentChatClient") ChatClient agentChatClient,
            @Qualifier("confirmationChatClient") ChatClient confirmationChatClient,
            InventoryService inventoryService) {
        this.agentChatClient = agentChatClient;
        this.confirmationChatClient = confirmationChatClient;
        this.inventoryService = inventoryService;
    }

    public String process(String message, String conversationId) {
        InventoryCommand pending = pendingCommands.get(conversationId);

        if (pending != null) {
            return handleConfirmation(message, conversationId, pending);
        }

        return handleNewIntent(message, conversationId);
    }

    private String handleNewIntent(String message, String conversationId) {
        InventoryCommand command = agentChatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(InventoryCommand.class);

        if (command == null) {
            return "Nao foi possivel identificar uma operacao de estoque valida. " +
                   "Por favor, informe o produto, a quantidade e a operacao desejada.";
        }

        return switch (command) {
            case UnknownCommand c  ->
                    "Nao foi possivel identificar uma operacao de estoque valida. " +
                    "Por favor, informe o produto, a quantidade e a operacao desejada.";
            case StockGetCommand c -> executeCommand(c);
            case StockInCommand c  -> { pendingCommands.put(conversationId, c); yield askConfirmation(c); }
            case StockOutCommand c -> { pendingCommands.put(conversationId, c); yield askConfirmation(c); }
        };
    }

    private String askConfirmation(InventoryCommand command) {
        String input = switch (command) {
            case StockInCommand c  -> String.format("Operacao: STOCK_IN | Produto: %s | Quantidade: %d", c.productId(), c.quantity());
            case StockOutCommand c -> String.format("Operacao: STOCK_OUT | Produto: %s | Quantidade: %d", c.productId(), c.quantity());
            default -> throw new IllegalStateException("Confirmacao nao aplicavel para: " + command);
        };

        return confirmationChatClient.prompt()
                .user(input)
                .call()
                .content();
    }

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

    private String executeCommand(InventoryCommand command) {
        try {
            return switch (command) {
                case StockInCommand c -> {
                    inventoryService.stockIn(c.productId(), c.quantity());
                    yield String.format("Entrada de %d unidades do produto '%s' registrada com sucesso.", c.quantity(), c.productId());
                }
                case StockOutCommand c -> {
                    inventoryService.stockOut(c.productId(), c.quantity());
                    yield String.format("Saida de %d unidades do produto '%s' registrada com sucesso.", c.quantity(), c.productId());
                }
                case StockGetCommand c -> inventoryService.listProducts().toString();
                case UnknownCommand c  -> "Nao foi possivel identificar uma operacao de estoque valida.";
                case null -> "Nao foi possivel identificar uma operacao de estoque valida.";
            };
        } catch (Exception e) {
            return String.format("Erro ao executar a operacao: %s", e.getMessage());
        }
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
