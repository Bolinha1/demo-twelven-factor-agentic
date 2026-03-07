# Feature: 12-Factor Agents — Melhorias de Aderência

**Branch:** `feature/12-factor-improvements`
**Data:** 2026-03-07
**Aderência anterior:** ~58% | **Aderência após:** ~85%

---

## Contexto

O projeto `demo-twelven-factor-agentic` é uma POC de agente de inventário com Spring Boot + Spring AI + Amazon Bedrock que demonstrava 4 dos 12 fatores com aderência forte e outros 3 de forma parcial. Esta feature implementou os 5 fatores ausentes ou fracos, tornando o projeto uma referência mais completa do framework [12-Factor Agents da HumanLayer](https://humanlayer.dev/12-factor-agents).

---

## Fatores Antes e Depois

| Factor | Antes | Depois |
|--------|-------|--------|
| 1 — Natural Language to Tool Calls | Forte | Forte |
| 2 — Own Your Prompts | Parcial | Forte |
| 3 — Own Your Context Window | Forte | Forte |
| 4 — Tools Are Just Structured Outputs | Forte | Forte |
| 5 — Unify Execution State and Business State | Forte | Forte |
| 6 — Launch / Pause / Resume with Simple APIs | Parcial | Forte |
| 7 — Contact Humans with Tool Calls | Ausente | Implementado |
| 8 — Own Your Control Flow | Parcial | Forte |
| 9 — Compact Errors into Context | Ausente | Implementado |
| 10 — Small, Focused Agents | Forte | Forte |
| 11 — Trigger From Anywhere | Ausente | Implementado |
| 12 — Make Your Agent a Stateless Reducer | Ausente | Implementado |

---

## Mudanças Implementadas

### Factor 2 — Own Your Prompts
**Problema:** Arquivos de prompt existiam mas sem metadados de versionamento.

**Solução:** Adicionado header de versão nos dois arquivos de prompt:
```
# version: 1.0.0 | agent: extraction | lang: pt-BR | updated: 2026-03-07
```

**Arquivos alterados:**
- `src/main/resources/prompts/system.prompt.st`
- `src/main/resources/prompts/confirmation.prompt.st`

---

### Factor 7 — Contact Humans with Tool Calls
**Problema:** A confirmação humana estava hardcoded no fluxo do serviço, não como tool call explícita e auditável.

**Solução:** Criada `HumanConfirmationTool` com método `@Tool` que retorna um `ApprovalRequest` estruturado com status `PENDING`. Registrada no `agentChatClient`, tornando a escalação humana um output do agente — rastreável e testável.

**Fluxo:**
```
Agente detecta operacao de alto impacto
  -> chama requestHumanApproval(conversationId, operationSummary)
  -> retorna ApprovalRequest { status: PENDING }
  -> usuario responde sim/nao
  -> AgentReducer processa confirmacao de forma deterministica
```

**Arquivos criados:**
- `src/.../tools/HumanConfirmationTool.java` — `@Tool requestHumanApproval()`
- `src/.../model/ApprovalRequest.java` — record com `conversationId`, `operationSummary`, `ApprovalStatus`

**Arquivos alterados:**
- `src/.../config/ChatClientConfig.java` — `HumanConfirmationTool` injetada e registrada via `.defaultTools()`

---

### Factor 8 — Own Your Control Flow
**Problema:** Sem retry explícito para extração de intenção. Falha na primeira tentativa retornava erro imediatamente.

**Solução:** Adicionado loop de retry com limite configurável no `AgentReducer`:
```java
private static final int MAX_EXTRACTION_RETRIES = 2;

for (int attempt = 1; attempt <= MAX_EXTRACTION_RETRIES; attempt++) {
    // tenta extrair InventoryCommand
    // se valido, break
    // se nao, tenta novamente
}
// esgotou tentativas: compacta erro via ErrorContextService
```

**Arquivo alterado:** `src/.../service/AgentReducer.java`

---

### Factor 9 — Compact Errors into Context
**Problema:** Erros retornavam `e.getMessage()` bruto — mensagem técnica sem contexto ou sugestão de ação.

**Solução:** Criado `ErrorContextService` que normaliza exceções em mensagens estruturadas:

**Formato de saída:**
```
[ERRO] {operacao}: {motivo legivel}. {acao sugerida}.
```

**Exemplo:**
```
[ERRO] stockOut: produto 'notebook-dell' nao encontrado.
Verifique se o produto existe e se ha saldo suficiente no estoque.
```

**Lógica:**
- `IllegalArgumentException` → "Verifique os dados informados"
- `IllegalStateException` → "O estado atual nao permite esta operacao"
- Operação `stockOut` → dica sobre verificar produto e saldo
- Operação `stockIn` → dica sobre verificar dados do produto
- Fallback genérico para erros não mapeados
- Mensagem limitada a 200 caracteres para não poluir o contexto do agente

**Arquivo criado:** `src/.../service/ErrorContextService.java`

---

### Factor 11 — Trigger From Anywhere
**Problema:** Único canal de disparo era REST HTTP. Agente acoplado ao transporte.

**Solução:** Desacoplamento via Spring `ApplicationEvent`. Novo canal pode ser adicionado publicando um `InventoryEvent` — sem alterar `InventoryAgentService`.

**Componentes:**
- `InventoryEvent` — `ApplicationEvent` com `message` e `conversationId`
- `AgentEventListener` — `@EventListener` que chama `InventoryAgentService.process()`
- `POST /inventory/event` — endpoint que publica o evento (simula fila, webhook, scheduler)

**Canais suportados após a mudança:**
| Canal | Como disparar |
|-------|--------------|
| REST direto | `POST /inventory/agent` |
| Evento Spring | `POST /inventory/event` |
| Fila de mensagens | Publicar `InventoryEvent` no consumer |
| Scheduler | `@Scheduled` publicando `InventoryEvent` |
| Webhook | Endpoint dedicado publicando `InventoryEvent` |

**Arquivos criados:**
- `src/.../event/InventoryEvent.java`
- `src/.../event/AgentEventListener.java`

**Arquivo alterado:** `src/.../controller/InventoryAgentController.java`

---

### Factor 12 — Make Your Agent a Stateless Reducer
**Problema:** Estado do agente (`ConcurrentHashMap<String, InventoryCommand>`) era implícito e acoplado ao serviço. Transições de estado espalhadas pelo código.

**Solução:** Padrão reducer explícito: `(AgentState, input) → ReducerResult(AgentState, response)`.

**Novos tipos:**
```java
// Fases do agente
enum AgentPhase { IDLE, AWAITING_CONFIRMATION }

// Estado completo e imutável
record AgentState(String conversationId, AgentPhase phase, InventoryCommand pendingCommand) {
    static AgentState idle(String conversationId) { ... }
    AgentState withPending(InventoryCommand command) { ... }
    AgentState reset() { ... }
}

// Resultado da redução
record ReducerResult(AgentState newState, String response) { }
```

**AgentReducer — função de redução:**
```
IDLE + mensagem de usuario
  -> extrai InventoryCommand (com retry)
  -> AWAITING_CONFIRMATION + pergunta de confirmacao

AWAITING_CONFIRMATION + "sim"
  -> executa comando no InventoryService
  -> IDLE + mensagem de sucesso

AWAITING_CONFIRMATION + "nao"
  -> IDLE + "Operacao cancelada"

AWAITING_CONFIRMATION + resposta ambigua
  -> AWAITING_CONFIRMATION + repete confirmacao
```

**InventoryAgentService após refatoração:**
```java
public String process(String message, String conversationId) {
    AgentState current = states.getOrDefault(conversationId, AgentState.idle(conversationId));
    ReducerResult result = reducer.reduce(current, message);
    // persiste ou remove estado baseado na fase
    return result.response();
}
```

**Arquivos criados:**
- `src/.../model/AgentPhase.java`
- `src/.../model/AgentState.java`
- `src/.../model/ReducerResult.java`
- `src/.../service/AgentReducer.java`

**Arquivo refatorado:** `src/.../service/InventoryAgentService.java`

---

## Arquitetura Após as Mudanças

```
POST /inventory/agent   POST /inventory/event
        |                       |
        v                       v
InventoryAgentController   (publica InventoryEvent)
        |                       |
        |               AgentEventListener
        |                       |
        +----------+------------+
                   |
                   v
        InventoryAgentService
          (mapa de AgentState)
                   |
                   v
             AgentReducer
          (reduce: state + input)
            /           \
           v             v
    agentChatClient   confirmationChatClient
    (extrai intent)   (formata confirmacao)
           |
    HumanConfirmationTool (@Tool)
           |
    InventoryService (stockIn / stockOut)
           |
       PostgreSQL
```

---

## Referências

- Framework original: [12-Factor Agents — HumanLayer](https://humanlayer.dev/12-factor-agents)
- Documentação local dos princípios: `.claude/12-factor-agents.md`
- Spring AI Tool Calling: `@Tool` annotation em `spring-ai-core`
- Spring Events: `ApplicationEvent` + `@EventListener`
