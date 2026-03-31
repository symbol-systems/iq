# FAQ & Troubleshooting

## Getting Started

### Q: Do I need Kubernetes or Docker?
**A:** No. You can:
- Run locally with `./bin/start-api` (dev mode)
- Use `docker-compose up` (all-in-one)
- Deploy to cloud (see [DEPLOYMENT.md](DEPLOYMENT.md))

Pick whatever fits your workflow.

### Q: What Java version do I need?
**A:** Java 21+. The Maven wrapper (`mvnw`) will auto-download OpenJDK if needed.

### Q: How much RAM does IQ need?
**A:** Minimum 2GB, recommended 4GB+. The default docker-compose allocates 2GB.

### Q: Can I use IQ without an LLM?
**A:** Yes! You can:
- Query via REST API: `curl http://localhost:8080/api/query`
- Use the CLI: `./bin/demo-query examples/queries/customers.sparql`
- Web UI at `http://localhost:8080/ui`

The MCP/LLM integration is optional.

---

## Common Issues

### "Port 8080 already in use"

**Solution:**
```bash
# Use a different port
./bin/start-api --port 8081

# Or kill the process using 8080
lsof -i :8080
kill -9 <PID>
```

### "Server didn't start" / "Connection refused"

**Diagnosis:**
```bash
# Check if it's still starting (wait ~30 sec on first run)
sleep 30 && curl http://localhost:8080/health

# Check logs
docker-compose logs iq-apis | tail -50

# Check Java errors
./mvnw compile quarkus:dev -pl iq-apis -am 2>&1 | grep -i error
```

**Common causes:**
- Insufficient RAM: increase in docker-compose or system settings
- Port conflict: see above
- Maven build issue: run `./mvnw -pl iq-apis -am clean compile`

### "MCP endpoints not found"

**Check:**
```bash
# Verify MCP is enabled
curl http://localhost:8080/health | jq '.mcp'
# Should return: true

# Check available endpoints
curl http://localhost:8080/mcp/tools | jq .
```

**If error:**
- Server may not have started with MCP enabled
- Check logs: `docker-compose logs iq-apis | grep -i mcp`
- Ensure `IQ_MCP_ENABLED=true` environment variable is set

### "SPARQL query returns empty results"

**Cause:** Knowledge graph is empty.

**Solution:**
```bash
# Load example data
./bin/import-example

# Or manually import RDF
curl -X POST http://localhost:8080/api/import \
  -H "Content-Type: application/x-turtle" \
  --data-binary @examples/data/customers.ttl

# Verify data was imported
curl http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"sparql": "SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"}'
```

### "jq: command not found"

**Solution:**
```bash
# Install jq
brew install jq  # macOS
apt-get install jq   # Ubuntu/Debian

# Or use Python instead
curl http://localhost:8080/mcp/tools | python3 -m json.tool
```

### "docker: command not found" or "docker-compose fails"

**Check installation:**
```bash
docker --version
docker-compose --version
```

**Install:**
- **macOS:** Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
- **Linux:** `sudo apt-get install docker.io docker-compose`
- **Windows:** Install [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)

### "Permission denied: ./bin/start-api"

**Solution:**
```bash
# Make scripts executable
chmod +x ./bin/start-api ./bin/demo-query ./bin/import-example

# Or run with bash
bash ./bin/start-api
```

---

## Performance & Scaling

### Q: Why is the first run slow?
**A:** IQ is JIT-compiling and indexing the knowledge graph. First run: ~30s, subsequent: ~5s.

### Q: How many triples can IQ handle?
**A:** 
- In-memory: 1-10 million triples (per 4GB RAM)
- RDF4J store: 100+ million triples
- External SPARQL endpoint: unlimited (depends on backend)

Configure storage:
```bash
# Use persistent RDF store instead of in-memory
IQ_KNOWLEDGE_GRAPH_TYPE=sparql-endpoint \
IQ_SPARQL_ENDPOINT=http://rdf4j-server:8080 \
./bin/start-api
```

### Q: Can I run multiple IQ instances?
**A:** Yes. Use a load balancer:

```yaml
# docker-compose.yml
services:
  iq-apis:
deploy:
  replicas: 3
  
  load-balancer:
image: nginx:latest
ports:
  - "8080:80"
environment:
  NGINX_HOSTS: "iq-apis:8080"
```

---

## Integration & Connectors

### Q: How do I add a connector?
**A:** See [CONNECTORS.md](CONNECTORS.md).

### Q: Can IQ sync with my database?
**A:** Yes. Use `iq-connect-jdbc` (generic SQL) or connector for your DB:

```bash
# Example: PostgreSQL
curl -X POST http://localhost:8080/api/import \
  -H "Content-Type: application/json" \
  -d '{
"connector": "jdbc",
"jdbc_url": "jdbc:postgresql://db:5432/mydb",
"user": "postgres",
"password": "secret",
"query": "SELECT * FROM customers"
  }'
```

### Q: How do I authenticate external APIs?
**A:** Use IQ's vault:

```bash
# Store API key
echo "your-api-key" > .iq/vault/external-api-key.secret

# Reference in RDF
@prefix vault: <http://systems.symbol/vault/> .
connector:MyAPI
connector:apiKey "secret:vault/external-api-key" .
```

---

## LLM & MCP Integration

### Q: Which LLMs support MCP?
**A:** 
- ✅ Claude (via Claude Desktop or web UI)
- ✅ ChatGPT (via Custom GPT with HTTP endpoint)
- ✅ Any LLM with MCP SDK support (Anthropic's SDK)
- ✅ Open-source: Llama, Mistral, etc. (with MCP client library)

### Q: How do I connect Claude to IQ?
**A:** See [MCP.md](MCP.md) → "Option A: Claude Desktop".

### Q: Can I use IQ with my own custom LLM?
**A:** Yes. Any LLM that can make HTTP POST requests can call IQ:

```
POST http://localhost:8080/mcp/tools/{tool_name}/execute
Content-Type: application/json

{ "param1": "value1", ... }
```

No MCP SDK required—just HTTP.

### Q: How do I prevent Claude from hallucinating?
**A:** 
- Make queries specific: "SELECT ?name WHERE { ?x type Customer }" (not open-ended)
- Provide good context in tool descriptions
- Use tool input schema to constrain inputs
- Test queries manually first

---

## Data & Storage

### Q: How do I backup my knowledge graph?
**A:**
```bash
# Docker: extract data volume
docker cp iq-apis:/app/data ./backup/iq-data

# Or tar it
docker exec iq-apis tar czf - /app/data > iq-backup.tar.gz

# Cloud: export to JSON-LD or N-Triples
curl http://localhost:8080/api/export?format=ntriples > graph.nt
```

### Q: How do I restore from backup?
**A:**
```bash
# Copy backup back
docker cp ./backup/iq-data iq-apis:/app/data

# Restart server
docker-compose restart iq-apis

# Or reimport
curl -X POST http://localhost:8080/api/import \
  -H "Content-Type: application/x-turtle" \
  --data-binary @backup.ttl
```

### Q: Can I export my data?
**A:** Yes:
```bash
# Export as Turtle (RDF/TTL)
curl http://localhost:8080/api/export?format=turtle > graph.ttl

# As N-Triples
curl http://localhost:8080/api/export?format=ntriples > graph.nt

# As JSON-LD
curl http://localhost:8080/api/export?format=jsonld > graph.json
```

---

## Security

### Q: Is my data secure?
**A:** Depends on your deployment:

| Setup | Security | Notes |
|-------|----------|-------|
| Local dev | Low | No TLS, no auth by default |
| Docker Compose | Medium | Add TLS + auth reverse proxy |
| Cloud (Azure/AWS) | High | TLS, managed identity, audit logs |

See [DEPLOYMENT.md](DEPLOYMENT.md) → "Security" for hardening steps.

### Q: How do I secure API keys?
**A:** Use the vault:

```bash
# Store in encrypted vault
echo "sk-abc123" > .iq/vault/openai-key.secret

# Reference in RDF (key never in plaintext)
<config/apis.ttl>
connector:OpenAI
connector:apiKey "secret:vault/openai-key" .

# Vault key is mastered in .iq/vault/master.key (not in git)
```

### Q: Can I use OAuth2 / OIDC?
**A:** Not yet. Coming soon. For now, use API keys + reverse proxy auth.

---

## Development & Debugging

### Q: How do I debug a SPARQL query?
**A:**
```bash
# Test query manually
./bin/demo-query examples/queries/my-query.sparql

# Enable debug logging
IQ_LOG_LEVEL=DEBUG ./bin/start-api 2>&1 | grep -i sparql

# View query execution plan
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: application/json" \
  -d '{"sparql": "SELECT ...'
```

### Q: How do I modify the knowledge graph programmatically?
**A:** Use REST API or CLI:

```bash
# REST: Add a statement
curl -X POST http://localhost:8080/api/add-statement \
  -H "Content-Type: application/json" \
  -d '{
"subject": "ex:Alice",
"predicate": "foaf:knows",
"object": "ex:Bob"
  }'

# CLI: quicker for local dev
./mvnw -pl iq-cli -am exec:java -Dexec.mainClass="systems.symbol.cli.IQ" \
  -Dexec.args="add ex:Alice foaf:knows ex:Bob"
```

### Q: How do I extend IQ?
**A:** 
- Add connectors: see `iq-connect/iq-connect-template/`
- Add tools: define in RDF, IQ auto-exposes via MCP
- Add workflows: model in RDF/Turtle
- Contribute: create a PR to symbol-systems/iq

---

## Still stuck?

**Check:**
1. [QUICKSTART.md](QUICKSTART.md) — 5-minute setup
2. [WHAT_IS_IQ.md](WHAT_IS_IQ.md) — Concepts & architecture
3. [USECASES.md](USECASES.md) — Real examples
4. [DEPLOYMENT.md](DEPLOYMENT.md) — Cloud setup
5. Open an issue: https://github.com/symbol-systems/iq/issues

**Debug logs:**
```bash
# Collect all logs
docker-compose logs > logs.txt

# Include system info
docker version >> logs.txt
docker-compose version >> logs.txt
java -version 2>> logs.txt

# Open issue with logs attached
```

**Community:** Ask on [Discussions](https://github.com/symbol-systems/iq/discussions)
