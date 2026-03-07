package com.twelvenfactoragentic.demotwelvenfactoragentic.controller;

import com.twelvenfactoragentic.demotwelvenfactoragentic.event.InventoryEvent;
import com.twelvenfactoragentic.demotwelvenfactoragentic.service.InventoryAgentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryAgentController {

    private final InventoryAgentService agent;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryAgentController(InventoryAgentService agent,
                                    ApplicationEventPublisher eventPublisher) {
        this.agent = agent;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Factor 6 — agente pode ser acionado via API
     * conversationId identifica a sessao do usuario para rastreamento de operacoes pendentes
     */
    @PostMapping("/agent")
    public String run(
            @RequestBody String message,
            @RequestParam(defaultValue = "default") String conversationId) {

        return agent.process(message, conversationId);
    }

    /**
     * Factor 11 — Trigger From Anywhere
     *
     * Canal adicional de disparo via evento Spring.
     * Simula recepcao de mensagem de uma fila, webhook ou qualquer outro canal externo.
     * O agente e invocado da mesma forma — sem conhecer a origem do evento.
     */
    @PostMapping("/event")
    public String publishEvent(
            @RequestBody String message,
            @RequestParam(defaultValue = "event-default") String conversationId) {

        eventPublisher.publishEvent(new InventoryEvent(this, message, conversationId));
        return "Evento publicado para conversationId: " + conversationId;
    }
}