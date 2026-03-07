package com.twelvenfactoragentic.demotwelvenfactoragentic.event;

import com.twelvenfactoragentic.demotwelvenfactoragentic.service.InventoryAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Factor 11 — Trigger From Anywhere
 *
 * Adapter que conecta o canal de eventos Spring ao agente de inventario.
 * O agente e invocado da mesma forma independente do canal de disparo:
 * REST, evento interno, fila de mensagens, scheduler, etc.
 *
 * Para adicionar um novo canal, basta publicar um InventoryEvent —
 * sem alterar a logica do agente.
 */
@Component
public class AgentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentEventListener.class);

    private final InventoryAgentService inventoryAgentService;

    public AgentEventListener(InventoryAgentService inventoryAgentService) {
        this.inventoryAgentService = inventoryAgentService;
    }

    @EventListener
    public void onInventoryEvent(InventoryEvent event) {
        log.info("InventoryEvent recebido: conversationId={}, message={}",
                event.getConversationId(), event.getMessage());

        String response = inventoryAgentService.process(event.getMessage(), event.getConversationId());

        log.info("Agente respondeu ao evento: conversationId={}, response={}",
                event.getConversationId(), response);
    }
}
