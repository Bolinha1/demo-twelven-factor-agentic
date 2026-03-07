package com.twelvenfactoragentic.demotwelvenfactoragentic.model;

/**
 * Factor 7 — Contact Humans with Tool Calls
 *
 * Representa uma solicitacao de aprovacao humana emitida pelo agente como output estruturado.
 * O status PENDING indica que o agente esta aguardando confirmacao do usuario.
 */
public record ApprovalRequest(
        String conversationId,
        String operationSummary,
        ApprovalStatus status
) {
    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}
