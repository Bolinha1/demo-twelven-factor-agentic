# Testes curl — Inventory Agent

Base URL: `http://localhost:8080`

---

## POST /inventory/agent — Fluxo principal (REST direto)

### 1. Entrada de estoque (STOCK_IN)

```bash
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-001" \
  -H "Content-Type: text/plain" \
  -d "Recebemos 10 notebooks Dell"
```

**Resposta esperada:** pergunta de confirmação de ENTRADA

---

### 2. Confirmar operação (sim)

```bash
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-001" \
  -H "Content-Type: text/plain" \
  -d "sim"
```

**Resposta esperada:** "Entrada de 10 unidades do produto 'notebooks Dell' registrada com sucesso."

---

### 3. Saída de estoque (STOCK_OUT)

```bash
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-002" \
  -H "Content-Type: text/plain" \
  -d "Saída de 5 monitores LG do estoque"
```

**Resposta esperada:** pergunta de confirmação de SAÍDA

---

### 4. Cancelar operação (nao)

```bash
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-002" \
  -H "Content-Type: text/plain" \
  -d "nao"
```

**Resposta esperada:** "Operacao cancelada. Nenhuma alteracao foi realizada no estoque."

---

### 5. Resposta ambígua (Factor 8 — controle de fluxo)

```bash
# Primeiro cria uma operacao pendente
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-003" \
  -H "Content-Type: text/plain" \
  -d "Entrada de 20 teclados mecânicos"

# Envia resposta ambigua
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-003" \
  -H "Content-Type: text/plain" \
  -d "talvez"
```

**Resposta esperada:** repete a pergunta de confirmação pedindo sim ou nao.

---

### 6. Mensagem sem intenção válida (Factor 9 — erro compactado)

```bash
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-004" \
  -H "Content-Type: text/plain" \
  -d "olá, bom dia"
```

**Resposta esperada:** mensagem solicitando produto, quantidade e operação.

---

### 7. Conversas independentes (Factor 3 — contexto isolado)

```bash
# Conversa A: cria operacao pendente
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-A" \
  -H "Content-Type: text/plain" \
  -d "Entrada de 30 mouses sem fio"

# Conversa B: operacao totalmente independente
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-B" \
  -H "Content-Type: text/plain" \
  -d "Saída de 8 headsets Jabra"

# Confirma conversa A sem afetar B
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-A" \
  -H "Content-Type: text/plain" \
  -d "confirmo"

# Cancela conversa B sem afetar A
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-B" \
  -H "Content-Type: text/plain" \
  -d "cancela"
```

---

## POST /inventory/event — Trigger via evento Spring (Factor 11)

### 8. Disparar agente via evento (simula fila / webhook)

```bash
curl -s -X POST "http://localhost:8080/inventory/event?conversationId=conv-evt-001" \
  -H "Content-Type: text/plain" \
  -d "Recebemos 50 cabos HDMI no depósito"
```

**Resposta esperada:** `"Evento publicado para conversationId: conv-evt-001"`

O agente processa o evento de forma assíncrona internamente via `AgentEventListener`.
Verificar o log da aplicação para ver a resposta do agente:
```
INFO AgentEventListener - InventoryEvent recebido: conversationId=conv-evt-001, message=...
INFO AgentEventListener - Agente respondeu ao evento: conversationId=conv-evt-001, response=...
```

---

### 9. Continuar conversa iniciada por evento via REST

```bash
# Inicia via evento
curl -s -X POST "http://localhost:8080/inventory/event?conversationId=conv-evt-002" \
  -H "Content-Type: text/plain" \
  -d "Entrada de 15 webcams Logitech"

# Confirma via REST (mesmo conversationId)
curl -s -X POST "http://localhost:8080/inventory/agent?conversationId=conv-evt-002" \
  -H "Content-Type: text/plain" \
  -d "sim"
```

**Demonstra:** Factor 11 + Factor 6 — o agente pode ser iniciado por qualquer canal e continuado por outro.

---

## Dicas de uso

### Formatar saída JSON (se o endpoint retornar JSON no futuro)
```bash
curl -s ... | python3 -m json.tool
```

### Adicionar verbose para depuração
```bash
curl -v -X POST "http://localhost:8080/inventory/agent?conversationId=debug-001" \
  -H "Content-Type: text/plain" \
  -d "Entrada de 5 notebooks"
```

### Script de smoke test completo
```bash
#!/bin/bash
BASE="http://localhost:8080"
CID="smoke-$(date +%s)"

echo "=== [1] Enviando intencao ==="
curl -s -X POST "$BASE/inventory/agent?conversationId=$CID" \
  -H "Content-Type: text/plain" \
  -d "Recebemos 10 notebooks Dell"

echo ""
echo "=== [2] Confirmando ==="
curl -s -X POST "$BASE/inventory/agent?conversationId=$CID" \
  -H "Content-Type: text/plain" \
  -d "sim"

echo ""
echo "=== [3] Testando canal de eventos ==="
curl -s -X POST "$BASE/inventory/event?conversationId=evt-$CID" \
  -H "Content-Type: text/plain" \
  -d "Saída de 3 notebooks Dell"

echo ""
echo "=== Smoke test concluido ==="
```
