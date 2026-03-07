package com.twelvenfactoragentic.demotwelvenfactoragentic.service;

import org.springframework.stereotype.Service;

/**
 * Factor 9 — Compact Errors into Context
 *
 * Normaliza excecoes em mensagens de contexto estruturadas e legiveis.
 * Nunca expoe stack traces ou mensagens tecnicas brutas ao usuario ou ao agente.
 *
 * Formato: [ERRO] {operacao}: {motivo legivel}. {acao sugerida}.
 */
@Service
public class ErrorContextService {

    public String compact(String operation, Exception e) {
        String reason = extractReason(e);
        String suggestion = suggestAction(operation, e);
        return String.format("[ERRO] %s: %s. %s", operation, reason, suggestion);
    }

    private String extractReason(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "falha inesperada sem detalhes adicionais";
        }
        String msg = e.getMessage();
        // Limita o tamanho para nao poluir o contexto do agente
        return msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }

    private String suggestAction(String operation, Exception e) {
        if (e instanceof IllegalArgumentException) {
            return "Verifique os dados informados e tente novamente";
        }
        if (e instanceof IllegalStateException) {
            return "O estado atual nao permite esta operacao. Consulte o estoque antes de tentar novamente";
        }
        if (operation != null && operation.contains("stockOut")) {
            return "Verifique se o produto existe e se ha saldo suficiente no estoque";
        }
        if (operation != null && operation.contains("stockIn")) {
            return "Verifique os dados do produto e tente novamente";
        }
        return "Tente novamente ou contate o suporte se o problema persistir";
    }
}
