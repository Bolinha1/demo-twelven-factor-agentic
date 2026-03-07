package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

/**
 * Factor 12 — Make Your Agent a Stateless Reducer
 *
 * Fases possiveis do agente de inventario.
 * Cada transicao de fase e explicita e auditavel.
 */
public enum AgentPhase {
    IDLE,
    AWAITING_CONFIRMATION
}
