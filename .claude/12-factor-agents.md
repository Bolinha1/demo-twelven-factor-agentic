# 12-Factor Agents

Referencia dos 12 principios para construcao de agentes de IA robustos e production-ready.
Baseado no framework da HumanLayer (humanlayer.dev/12-factor-agents).

Adaptado para o stack: Java 21 + Spring Boot + Spring AI + Amazon Bedrock.

---

## Factor 1 — Natural Language to Tool Calls

**Principio:** O agente deve converter linguagem natural em chamadas de ferramentas estruturadas, nao em codigo arbitrario.

**O que fazer:**
- Receba texto livre do usuario
- Use o LLM apenas para extrair a intencao como output estruturado (ex: record Java, JSON schema)
- Execute a logica de negocio fora do LLM, via chamadas de ferramentas deterministas

**O que evitar:**
- Deixar o LLM tomar decisoes de negocio diretamente
- Gerar e executar codigo dinamico como resposta

**Neste projeto:** `InventoryAgentService.handleNewIntent()` extrai `InventoryCommand` via structured output. O LLM decide "o que fazer", nao "como fazer".

---

## Factor 2 — Own Your Prompts

**Principio:** Prompts sao codigo. Versione, teste e gerencie-os como artefatos de primeira classe.

**O que fazer:**
- Armazene prompts em arquivos versionados junto ao codigo (`.st`, `.md`, `.txt`)
- Inclua metadados: versao, agente alvo, idioma
- Teste prompts isoladamente com entradas e saidas esperadas
- Evolua prompts com commits atomicos e mensagens descritivas

**O que evitar:**
- Prompts hardcoded em codigo Java
- Strings de prompt sem versionamento ou rastreabilidade
- Alterar prompts sem registrar o motivo da mudanca

**Header obrigatorio nos arquivos de prompt:**
```
# version: X.Y.Z | agent: <nome> | lang: pt-BR | updated: YYYY-MM-DD
```

**Neste projeto:** `system.prompt.st` e `confirmation.prompt.st` em `src/main/resources/prompts/`.

---

## Factor 3 — Own Your Context Window

**Principio:** Controle explicitamente o que entra na janela de contexto do LLM. Nao deixe o framework decidir por voce.

**O que fazer:**
- Persista historico de conversacao em armazenamento externo (banco de dados, nao memoria JVM)
- Use janela deslizante com limite configuravel de mensagens
- Inclua no contexto apenas o que e relevante para a decisao atual
- Limpe o contexto ao final de fluxos completos

**O que evitar:**
- Historico ilimitado que aumenta custo e degrada qualidade
- Armazenamento de contexto apenas em memoria (perde-se no restart)
- Deixar o framework acumular mensagens sem controle

**Neste projeto:** `ChatMemoryConfig` cria `MessageWindowChatMemory` com `JdbcChatMemoryRepository`. Contexto persistido em PostgreSQL, isolado por `conversationId`.

---

## Factor 4 — Tools Are Just Structured Outputs

**Principio:** Ferramentas sao outputs estruturados do LLM, nao extensoes magicas. O agente declara a intencao; o codigo a executa.

**O que fazer:**
- Defina ferramentas com `@Tool` como metodos Java com parametros tipados
- O retorno da ferramenta deve ser um record ou tipo simples serializavel
- Separe a decisao (LLM) da execucao (codigo Java)
- Documente o que cada ferramenta faz, quando usá-la e o que retorna

**O que evitar:**
- Ferramentas com logica de negocio complexa dentro delas
- Ferramentas que chamam o LLM internamente
- Side effects nao documentados em ferramentas

**Neste projeto:** `InventoryTools.java` declara `@Tool` methods. A execucao real e em `InventoryService` (chamado apos confirmacao humana).

---

## Factor 5 — Unify Execution State and Business State

**Principio:** Estado de execucao do agente e estado de negocio devem coexistir no mesmo armazenamento confiavel.

**O que fazer:**
- Use o mesmo banco de dados para entidades de negocio e estado do agente
- Trate transacoes de negocio e transicoes de estado do agente atomicamente quando possivel
- Evite estado implicito espalhado em variaveis de instancia

**O que evitar:**
- Estado do agente em memoria JVM (nao sobrevive a restarts)
- Estado de negocio e estado do agente em bancos diferentes sem sincronizacao
- Inconsistencias entre o que o agente "acha" e o que o banco "tem"

**Neste projeto:** `Product` (JPA, PostgreSQL) para negocio. `spring_ai_chat_memory` (JDBC, PostgreSQL) para execucao. Mesmo banco, schemas separados.

---

## Factor 6 — Launch / Pause / Resume with Simple APIs

**Principio:** O ciclo de vida do agente deve ser controlavel via APIs simples e sem estado proprio.

**O que fazer:**
- Exponha o agente via API REST simples (POST com texto + conversationId)
- Use `conversationId` para retomar conversas pausadas
- Permita que o agente seja interrompido e retomado sem perder estado
- Documente o contrato da API (entrada, saida, erros esperados)

**O que evitar:**
- Sessoes HTTP com estado (sessions, cookies)
- Agentes que nao podem ser interrompidos no meio de um fluxo
- Dependencia de conexao persistente entre cliente e servidor

**Neste projeto:** `POST /inventory/agent?conversationId={id}` com corpo texto plano. `conversationId` opcional (default gerado pelo sistema).

---

## Factor 7 — Contact Humans with Tool Calls

**Principio:** Quando o agente precisa de aprovacao humana, deve solicitá-la via tool call estruturada — nao via texto livre.

**O que fazer:**
- Crie uma ferramenta explícita (ex: `requestHumanApproval`) para escalacao humana
- O retorno da ferramenta deve ser um record com status (`PENDING`, `APPROVED`, `REJECTED`)
- Registre todas as solicitacoes de aprovacao para auditoria
- Defina timeout e comportamento padrao para aprovacoes nao respondidas

**O que evitar:**
- Confirmacoes hardcoded no fluxo de controle do servico
- Perguntas de confirmacao via texto livre sem estrutura
- Aprovacoes sem rastreabilidade

**Neste projeto:** `HumanConfirmationTool.requestHumanApproval()` com retorno `ApprovalRequest`. Registrado no `agentChatClient` em `ChatClientConfig`.

---

## Factor 8 — Own Your Control Flow

**Principio:** O fluxo de controle do agente deve ser explícito, determinista e testavel — nao delegado implicitamente ao LLM.

**O que fazer:**
- Implemente retry com limite configuravel para operacoes que podem falhar
- Defina caminhos de recuperacao explícitos para cada tipo de falha
- Documente o fluxo de estados do agente (maquina de estados)
- Use circuit breaker para dependencias externas (LLM, banco)

**O que evitar:**
- Deixar o LLM decidir quando e quantas vezes tentar novamente
- Fluxo implicito onde erros simplesmente "passam adiante"
- Loop infinito de retry sem limite ou backoff

**Constante recomendada:**
```java
private static final int MAX_EXTRACTION_RETRIES = 2;
```

**Neste projeto:** `InventoryAgentService.handleNewIntent()` com retry configuravel. Fallback via `ErrorContextService` apos esgotar tentativas.

---

## Factor 9 — Compact Errors into Context

**Principio:** Erros devem ser normalizados e compactados em contexto util antes de retornar ao agente ou ao usuario.

**O que fazer:**
- Crie um servico dedicado para normalizacao de erros (`ErrorContextService`)
- Inclua: operacao que falhou, motivo legivel, acao sugerida
- Formate erros de forma que o LLM possa usá-los para decidir o proximo passo
- Nunca retorne stack traces ou mensagens tecnicas brutas ao usuario

**Formato recomendado:**
```
[ERRO] {operacao}: {motivo legivel}. {acao sugerida}.
```

**O que evitar:**
- `e.getMessage()` direto no response
- Stack traces expostos ao usuario ou ao LLM
- Erros genéricos sem contexto da operacao que falhou

**Neste projeto:** `ErrorContextService.compact(operation, exception)` usado em `InventoryAgentService.executeCommand()`.

---

## Factor 10 — Small, Focused Agents

**Principio:** Cada agente deve ter uma responsabilidade clara e delimitada. Prefira multiplos agentes pequenos a um agente monolitico.

**O que fazer:**
- Defina o escopo do agente explicitamente no system prompt
- Um agente por dominio de negocio (inventario, financeiro, logistica, etc.)
- Use agentes separados para tarefas distintas (extracao, confirmacao, execucao)
- Documente o que o agente NAO faz

**O que evitar:**
- Agentes que fazem tudo (extracao + validacao + execucao + notificacao)
- System prompts com multiplas responsabilidades conflitantes
- Agentes que chamam outros agentes sem controle de fluxo explícito

**Neste projeto:** Dois ChatClients distintos — `agentChatClient` (extracao de intencao) e `confirmationChatClient` (formatacao de confirmacao). Responsabilidades separadas.

---

## Factor 11 — Trigger From Anywhere

**Principio:** O agente deve poder ser disparado por qualquer canal sem alterar sua logica interna.

**O que fazer:**
- Desacople o transporte (HTTP, fila, evento, scheduler) da logica do agente
- Use um ponto de entrada unico no servico (`process(message, conversationId)`)
- Adicione adapters para cada canal (REST controller, event listener, scheduled job)
- Documente todos os pontos de entrada suportados

**O que evitar:**
- Logica de negocio do agente dentro do controller HTTP
- Acoplamento entre o canal de disparo e a logica de processamento
- Canais de disparo que nao sao testáveis independentemente

**Canais suportados neste projeto:**
- REST: `POST /inventory/agent`
- Evento Spring: `InventoryEvent` consumido por `AgentEventListener`
- (Extensivel para: Kafka consumer, webhook, @Scheduled, SQS listener)

---

## Factor 12 — Make Your Agent a Stateless Reducer

**Principio:** O agente deve funcionar como um reducer puro: `(estado, entrada) -> (novo estado, saida)`. Sem estado implicito.

**O que fazer:**
- Defina `AgentState` como record imutavel com todos os campos de estado
- Implemente `AgentReducer.reduce(state, input) -> ReducerResult`
- Persista `AgentState` externamente (banco, cache distribuido)
- Trate o reducer como funcao pura: mesma entrada + mesmo estado = mesma saida

**O que evitar:**
- Estado do agente em variaveis de instancia do servico Spring
- Transicoes de estado implicitas espalhadas pelo codigo
- Estado que nao pode ser inspecionado ou serializado

**Estrutura recomendada:**
```java
record AgentState(String conversationId, AgentPhase phase, InventoryCommand pendingCommand) {}
record ReducerResult(AgentState newState, String response) {}

// AgentReducer
ReducerResult reduce(AgentState state, String input) { ... }
```

**Neste projeto:** `AgentReducer` recebe `AgentState` + input, retorna `ReducerResult`. `InventoryAgentService` mantem o mapa de estados e delega transicoes ao reducer.
