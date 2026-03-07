# demo-twelven-factor-agentic

Spring Boot demo showcasing the **12-Factor Agentic** principles using Spring AI and Amazon Bedrock.

## Stack

- Java 21 + Spring Boot 3.3.5
- Spring AI 1.1.1 — Amazon Bedrock (Nova Pro)
- PostgreSQL — business data (JPA) + chat memory (JDBC)

---

## Prerequisites

- Docker
- Java 21
- AWS credentials with access to Amazon Bedrock (region `us-east-1`)

---

## Running

**1. Configure environment variables**

```bash
cp .env-example .env
# Edit .env with your AWS credentials
```

**2. Start the database**

```bash
docker compose up -d
```

**3. Run the application**

```bash
./mvnw spring-boot:run
```

The application starts at `http://localhost:8080`.

---

## Usage

```
POST /inventory/agent?conversationId={id}
Content-Type: text/plain
Body: natural language message
```

The flow is always two steps: send a command → confirm or cancel.

**Stock in:**
```bash
curl -X POST "http://localhost:8080/inventory/agent?conversationId=user1" \
  -H "Content-Type: text/plain" \
  -d "We received 10 dell notebooks"

curl -X POST "http://localhost:8080/inventory/agent?conversationId=user1" \
  -H "Content-Type: text/plain" \
  -d "yes"
```

**Stock out:**
```bash
curl -X POST "http://localhost:8080/inventory/agent?conversationId=user2" \
  -H "Content-Type: text/plain" \
  -d "Remove 5 monitors"

curl -X POST "http://localhost:8080/inventory/agent?conversationId=user2" \
  -H "Content-Type: text/plain" \
  -d "no"
```
