package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

/**
 * Factor 12 — Make Your Agent a Stateless Reducer
 *
 * Resultado de uma reducao: novo estado + resposta ao usuario.
 * Permite que o chamador persista newState e retorne response de forma desacoplada.
 */
public record ReducerResult(AgentState newState, String response) {
}
