package com.twelvenfactoragentic.demotwelvenfactoragentic.tools;

import com.twelvenfactoragentic.demotwelvenfactoragentic.model.ApprovalRequest;
import com.twelvenfactoragentic.demotwelvenfactoragentic.model.ApprovalRequest.ApprovalStatus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Factor 7 — Contact Humans with Tool Calls
 *
 * Escalacao humana modelada como tool call explícita e auditavel.
 * O agente nao executa operacoes de escrita sem aprovacao — ele emite
 * um ApprovalRequest estruturado e aguarda resposta do usuario.
 *
 * Isso torna a interacao humana-no-loop um output do agente,
 * nao um efeito colateral hardcoded no fluxo de controle.
 */
@Component
public class HumanConfirmationTool {

    /**
     * Solicita aprovacao humana para uma operacao de alto impacto.
     *
     * @param conversationId  identificador da conversa em andamento
     * @param operationSummary descricao legivel da operacao a ser confirmada
     * @return ApprovalRequest com status PENDING, aguardando confirmacao do usuario
     */
    @Tool(description = "Solicita aprovacao humana antes de executar uma operacao de estoque. " +
            "Use esta ferramenta sempre que precisar confirmar uma acao irreversivel com o usuario.")
    public ApprovalRequest requestHumanApproval(String conversationId, String operationSummary) {
        return new ApprovalRequest(conversationId, operationSummary, ApprovalStatus.PENDING);
    }
}
