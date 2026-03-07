package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

/**
 * Factor 12 — Make Your Agent a Stateless Reducer
 *
 * Representa o estado completo do agente para uma determinada conversa.
 * Imutavel: toda transicao de estado produz um novo AgentState.
 *
 * Estado inicial: AgentState.idle(conversationId)
 */
public record AgentState(
        String conversationId,
        AgentPhase phase,
        InventoryCommand pendingCommand
) {
    public static AgentState idle(String conversationId) {
        return new AgentState(conversationId, AgentPhase.IDLE, null);
    }

    public AgentState withPending(InventoryCommand command) {
        return new AgentState(this.conversationId, AgentPhase.AWAITING_CONFIRMATION, command);
    }

    public AgentState reset() {
        return new AgentState(this.conversationId, AgentPhase.IDLE, null);
    }
}
