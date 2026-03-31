# QUICKSTART — 5 minutes to your first IQ query

## Prerequisites

- Java 21+ (Maven will download if needed)
- curl (to test APIs)
- 4GB free RAM

## Step 1: Start the API server (2 minutes)

```bash
cd /path/to/iq/iq-starter
./bin/start-api
```

You'll see:
```
2024-03-31 12:34:56.789 INFO  IQ API server started on port 8080
2024-03-31 12:34:57.123 INFO  Knowledge graph initialized
2024-03-31 12:34:57.456 INFO  3 realms loaded, 2,847 facts indexed
```

The server is ready when you see:
```
2024-03-31 12:34:58 INFO  HTTP server listening on 0.0.0.0:8080
```

**If startup takes >30 seconds:** this is normal for the first run. Java is compiling and indexing your knowledge graph. Subsequent starts will be faster.

## Step 2: Test with a chat message (1 minute)

Open a **new terminal** and run:

```bash
./bin/demo-chat "What can IQ do for me?"
```

You should get a response like:
```json
{
  "response": "IQ is a knowledge execution platform that turns structured knowledge and AI 
  into executable, stateful workflows...",
  "model": "gpt-3.5-turbo",
  "tokens_used": 87
}
```

If you get an error about `OPENAI_API_KEY`, see [FAQ](FAQ.md#no-openai-key).

## Step 3: Trigger an agent workflow (1 minute)

Agents are stateful AI workers that make multi-step decisions. Try:

```bash
./bin/demo-agent "process customer request" --customer-id=12345
```

Expected flow:
1. Agent reads the customer record
2. Decides whether to approve, deny, or escalate
3. Updates state machine
4. Calls connectors (email, Slack, etc.)

You'll see the full decision trace in the response.

## Step 4: Query your knowledge graph (1 minute)

Run a SPARQL query:

```bash
./bin/demo-query examples/queries/00-hello.sparql
```

Output:
```
?subject | ?predicate | ?object
---------|-----------|--------
http://example.com/alice | foaf:name | "Alice"
http://example.com/bob | foaf:name | "Bob"
...
```

---

## What just happened?

| Action | What it touched |
|---|---|
| `./bin/start-api` | Started the REST server, loaded 3 test realms, indexed knowledge |
| `./bin/demo-chat` | Sent a message to the chat API, which used your LLM to ground the answer in knowledge |
| `./bin/demo-agent` | Executed a state machine with decision logic and side effects |
| `./bin/demo-query` | Ran SPARQL directly against your knowledge graph (no LLM needed) |

## Next steps

- **Add your own data:** [Ingest knowledge](docs/INGESTION.md)
- **Explore use cases:** [Use Cases](docs/USECASES.md)
- **Connect to systems:** [Connector Setup](docs/CONNECTORS.md)
- **Build workflows:** [Agent & FSM Guide](docs/AGENTS.md)
- **Deploy to cloud:** [Docker & Cloud](docs/DOCKER.md)

---

## Troubleshooting this quickstart

**"Port 8080 already in use"**
```bash
# Use a different port
./bin/start-api --port 8081
```

**"No OpenAI key"**
See [FAQ: Using without OpenAI](FAQ.md#no-openai).

**"Knowledge graph not loading"**
Check `.iq/repositories/default/` exists. If not:
```bash
rm -rf .iq/repositories
./bin/import-example  # Reinitialize
```

**"Queries return empty"**
Your knowledge graph might be empty. Load examples:
```bash
./bin/import-example
```

---

**Completed?** Now explore [Use Cases](docs/USECASES.md) to see what you can build. 🚀
