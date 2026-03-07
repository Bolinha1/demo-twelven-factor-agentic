package com.twelvenfactoragentic.demotwelvenfactoragentic.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.twelvenfactoragentic.demotwelvenfactoragentic.service.InventoryAgentService;

@RestController
@RequestMapping("/inventory")
public class InventoryAgentController {

    private final InventoryAgentService agent;

    public InventoryAgentController(InventoryAgentService agent) {
        this.agent = agent;
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
}