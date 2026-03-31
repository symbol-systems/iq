# FAQ — Troubleshooting & common questions

## Getting started

### Q: Do I need a credit card to start?
**A:** No. If you use IQ **without** OpenAI, it's free (uses local models or SPARQL-only). If you use OpenAI, you need an API key, which requires a card on file but no upfront charges.

### Q: How long does it take to get running?
**A:** 5 minutes. See [QUICKSTART.md](QUICKSTART.md).

### Q: Can I run this without an LLM (AI model)?
**A:** Yes. You can use IQ purely for knowledge graphs and queries (SPARQL). The LLM is optional. See "Using without OpenAI" below.

### Q: What if I don't have an OpenAI API key?
**A:** Options:
1. **Use Groq** (free, ultra-fast): Set `IQ_LLM_PROVIDER=groq`
2. **Use local models**: Install Ollama, set `IQ_LLM_PROVIDER=local`
3. **Use knowledge-only mode**: No LLM at all, just queries
4. **Ask for organizational key**: If you're evaluating IQ at work, ask your boss

---

## Common errors

### "Port 8080 already in use"

```bash
# Option 1: Kill the process using 8080
lsof -i :8080
kill -9 <PID>

# Option 2: Use a different port
./bin/start-api --port 8081
```

### "OPENAI_API_KEY not set" or "OpenAI request failed"

```bash
# Set your key
export OPENAI_API_KEY=sk-your-key-here

# Then start
./bin/start-api
```

If you don't have a key:

```bash
# Use Groq instead (free)
export GROQ_API_KEY=gsk-...
export IQ_LLM_PROVIDER=groq

./bin/start-api
```

Or run knowledge-only mode:

```bash
export IQ_LLM_PROVIDER=none
./bin/start-api
```

### "Knowledge graph not loading"

```bash
# Check if the directory exists
ls -la .iq/repositories/default/

# If it doesn't exist, create it
mkdir -p .iq/repositories/default

# Then import example data
./bin/import-example

# Restart
./bin/start-api
```

### "Connection refused" when trying to POST to the API

**Symptom:**
```
curl: (7) Failed to connect to localhost port 8080: Connection refused
```

**Solution:**
```bash
# Make sure the server is running
./bin/start-api

# In another terminal:
curl http://localhost:8080/health

# Should return: {"status":"UP"}
```

### "Query returns empty results"

**Your knowledge graph might be empty.** Load examples:

```bash
# Load sample data
./bin/import-example

# Verify it worked
iq> query examples/queries/00-hello.sparql
```

### "Agent trigger returns 404"

```bash
# Agents must be defined in .iq-templates/ first

# Import example agents
./bin/setup-agents examples/agents/

# Then trigger
curl -X POST http://localhost:8080/agent/trigger ...
```

### Memory issues or slow startup

```bash
# IQ is indexing on first run (can take 1-5 minutes on cold start)
# Subsequent starts are faster

# If it's taking >10 minutes:
# Check available RAM: free -h
# Reduce knowledge scope: ./bin/import-example (instead of full import)
```

---

## Connectors & integrations

### Q: How do I connect my Slack workspace?

```bash
# 1. Create an app: https://api.slack.com/apps
# 2. Get Bot Token (xoxb-...)
# 3. Set it
export SLACK_BOT_TOKEN=xoxb-your-token

# 4. Configure which channels
./bin/setup-connectors examples/connectors/slack/

# 5. Start and sync
./bin/start-api
iq> connector sync slack-connector
```

### Q: Do connectors sync in real-time?

**No, connectors sync on a schedule** (every N hours/days). This is intentional:
- Avoids overwhelming external APIs
- Keeps costs low
- Prevents rate limiting

To sync manually:

```bash
iq> connector read slack #engineering \
      --into-realm my-project \
      --now
```

### Q: What if a connector fails?

Check logs:

```bash
iq> connector logs <name>
```

Common issues:
- **Auth failed** → Check credentials (token, API key, connection string)
- **Network error** → Check firewall, VPN, proxy
- **Rate limited** → Wait, or reduce frequency
- **Schema changed** → Connector needs update

---

## Workflows & agents

### Q: How do I know if an agent succeeded?

```bash
# Check the response
curl -X POST http://localhost:8080/agent/trigger \
  -d '{"intent":"my-agent","object_id":"abc"}'

# Response includes:
# - "final_state": the new state
# - "actions_executed": list of actions + status
# - "decision_reasoning": why this decision was made
```

### Q: Can I undo a decision?

**Depends on the state machine.** If the FSM allows backwards transitions:

```bash
curl -X POST http://localhost:8080/agent/trigger \
  -d '{"intent":"revert-approval","request_id":"po-98765"}'
```

Otherwise, the decision is permanent. This is intentional (audit trail).

### Q: How do I debug why a rule didn't match?

```bash
iq> agent trace po-98765

# Output shows:
# - Which rules evaluated
# - Which rules matched
# - Which rule won
# - Why others didn't match
```

---

## Performance & scaling

### Q: How many queries per second can IQ handle?

**Depends on:**
- Hardware (faster CPU → more QPS)
- Query complexity (simple SPARQL is fast, complex joins are slower)
- Replica count (in Kubernetes)

**Rough numbers:**
- Single instance on a laptop: 50-100 QPS
- 3 replicas on standard cloud VM: 500-1000 QPS
- 10 replicas on beefy hardware: 2000+ QPS

To exceed this, shard your knowledge graphs (different realms for different data).

### Q: How much data can IQ hold?

**Default (in-memory): 1-5 GB** of RDF facts depending on RAM.

**With PostgreSQL backend: unlimited** (same as your database).

Switch to PostgreSQL for production:

```bash
export DATABASE_URL=postgresql://user:pass@db.example.com/iq

./bin/start-api
```

### Q: What's the search latency?

**Latency:**
- Knowledge lookup: < 1ms
- Simple SPARQL query: 5-50ms
- Complex LLM query: 500-2000ms (LLM inference time dominates)

### Q: Can I cache results?

**Sort of.** IQ caches frequently-run queries. For custom caching:

```bash
# Store result as a fact
iq> query --cache 3600 my-query.sparql
# Returned result is cached for 1 hour
```

---

## Data & privacy

### Q: Is my data sent to OpenAI?

**Only what you ask the LLM.** Specifically:
- Your knowledge facts: **NOT sent** (unless you explicitly quote them in a prompt)
- Your question: **SENT** to OpenAI (unless using local LLM)
- Your connector data: **STAYS LOCAL** (never sent to OpenAI)

See privacy policy at https://symbol.systems/privacy

### Q: Can I use IQ with a local LLM (not OpenAI)?

**Yes.**

```bash
# Install Ollama: https://ollama.ai

# Start Ollama
ollama serve

# In another terminal:
export IQ_LLM_PROVIDER=local
export IQ_LLM_MODEL=llama2

./bin/start-api
```

Fully private, no external API calls.

### Q: Can I export my data?

```bash
# Export entire realm
iq> export-realm my-realm result.ttl

# Export subset
iq> query examples/queries/export-customers.sparql --output result.ttl

# Export to CSV
iq> query examples/queries/customers.sparql --output result.csv
```

---

## Organization & deployment

### Q: Can multiple teams share one IQ instance?

**Yes, via realms.** Each team/project gets its own realm:

```bash
export MY_IQ=acme-corp
./bin/start-api

# Realm 1: acme-corp
# Realm 2: acme-finance
# Realm 3: acme-marketing

curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/chat \
  -d '{"realm":"acme-finance", "message":"..."}'
```

Realms are isolated (data, secrets, rules).

### Q: How do I deploy to production?

See [DOCKER.md](docs/DOCKER.md) for Docker, Kubernetes, AWS, Azure, GCP.

### Q: How do I backup my data?

**Docker:**
```bash
docker cp iq-container:/opt/iq/.iq ./iq-backup
```

**Kubernetes:**
```bash
kubectl exec -it pod/iq-api -n iq-system -- \
  tar czf /backup.tar.gz /opt/iq/.iq
kubectl cp iq-system/iq-api:/backup.tar.gz ./iq-backup.tar.gz
```

**PostgreSQL:**
```bash
# Use your database's backup tool
pg_dump iq_prod > backup.sql
```

### Q: How do I upgrade IQ?

```bash
# Pull latest
git pull origin main

# Rebuild
./mvnw clean install -DskipTests

# Restart
./bin/start-api
```

For production: test in staging first.

---

## Support & community

### Q: Where can I ask for help?

- **GitHub Issues:** https://github.com/symbol-systems/iq/issues
- **Community:** https://symbol.systems/community
- **Docs:** https://symbol.systems/docs

### Q: How do I report a bug?

```bash
# Gather info
./bin/iq-debug-report

# Include:
# 1. What you tried
# 2. What happened
# 3. What you expected
# 4. The error message (and logs)
# 5. Your IQ version

# File issue at: https://github.com/symbol-systems/iq/issues
```

### Q: Can I contribute?

Yes! See [CONTRIBUTING.md](../iq-docs/docs/) in the repo.

---

## Performance tuning

### Slow queries?

```bash
# Explain what's slow
iq> explain-query examples/queries/my-query.sparql

# Output shows:
# - Query plan
# - Estimated cost
# - Suggestions to optimize
```

### High CPU?

```bash
# Check what's running
iq> list-running-tasks

# If something is stuck:
iq> cancel-task <task-id>
```

---

## Still stuck?

```bash
# Generate a debug bundle
./bin/iq-debug-report

# Output includes:
# - Java version
# - Available memory
# - Installed connectors
# - Recent logs
# - Config (minus secrets)

# Share with the team on GitHub Issues
```

---

**Next:** Back to [QUICKSTART.md](QUICKSTART.md) or explore [USECASES.md](USECASES.md) 🚀

