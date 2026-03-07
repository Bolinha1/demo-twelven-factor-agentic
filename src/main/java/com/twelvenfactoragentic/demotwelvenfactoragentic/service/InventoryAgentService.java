package com.twelvenfactoragentic.demotwelvenfactoragentic.service;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.AgentPhase;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.AgentState;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.ReducerResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factor 1  — Linguagem natural -> pipeline estruturado de dois estagios.
 * Factor 6  — Agente acessivel via API simples (REST, eventos, etc.).
 * Factor 12 — Stateless reducer: este servico apenas mantem o mapa de estados
 *             e delega toda logica de transicao ao AgentReducer.
 *
 * Fluxo:
 *   process(input, conversationId)
 *     -> carrega AgentState do mapa (ou cria IDLE)
 *     -> AgentReducer.reduce(state, input) -> ReducerResult
 *     -> persiste newState no mapa
 *     -> retorna response ao chamador
 */
@Service
public class InventoryAgentService {

    /**
     * Factor 12 — estado explicito por conversationId.
     * Substitui o ConcurrentHashMap<String, InventoryCommand> anterior,
     * agora carregando o estado completo do agente (fase + comando pendente).
     */
    private final Map<String, AgentState> states = new ConcurrentHashMap<>();

    private final AgentReducer reducer;

    public InventoryAgentService(AgentReducer reducer) {
        this.reducer = reducer;
    }

    public String process(String message, String conversationId) {
        AgentState current = states.getOrDefault(conversationId, AgentState.idle(conversationId));

        ReducerResult result = reducer.reduce(current, message);

        // Persiste o novo estado (Factor 12: transicao explicita e auditavel)
        if (result.newState().phase() == AgentPhase.IDLE) {
            states.remove(conversationId);
        } else {
            states.put(conversationId, result.newState());
        }

        return result.response();
    }
}
