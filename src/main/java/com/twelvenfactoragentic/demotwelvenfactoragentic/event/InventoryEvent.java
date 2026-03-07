package com.twelvenfactoragentic.demotwelvenfactoragentic.event;

import org.springframework.context.ApplicationEvent;

/**
 * Factor 11 — Trigger From Anywhere
 *
 * Evento Spring que representa uma mensagem de inventario recebida por qualquer canal
 * (fila de mensagens, webhook, job agendado, etc.).
 *
 * O agente nao sabe — nem precisa saber — qual canal originou o evento.
 */
public class InventoryEvent extends ApplicationEvent {

    private final String message;
    private final String conversationId;

    public InventoryEvent(Object source, String message, String conversationId) {
        super(source);
        this.message = message;
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public String getConversationId() {
        return conversationId;
    }
}
