# IQ User Manual (CLI + TTL)

This manual covers the current IQ workspace features exposed by:
- `iq-cli` (community CLI)
- `iq-cli-pro` (enterprise extensions)
- `iq-cli-server` (runtime manager)
- `iq-camel` (route/ACL policy)
- `iq-mcp` (MCP tool server)
- `iq-secrets` (secret backends)

## Quick start

1. Start from repo root:
   ```bash
   ./mvnw -DskipITs=true compile
   ./mvnw -DskipITs=true -pl iq-cli,iq-cli-pro,iq-cli-server -am test
   ```
2. Create a home folder:
   ```bash
   mkdir -p ~/iq-data
   export IQ_HOME=~/iq-data
   ```
3. Initialize:
   ```bash
   ./bin/iq init
   ./bin/iq status
   ```

## Editions and command surface

### `iq-cli` (Community)

Main commands:
- `init`  : bootstrap workspace + repository
- `list`  : show graph triples / configs
- `script <file.sparql>` : execute SPARQL script
- `sparql <query>` : run inline query
- `agent` : list/trigger agents
- `render` : render graph content
- `infer` : run inference bundle from script
- `import` / `export` : load/save graphs
- `backup` / `recover` : backup and restore KB
- `server` : runtime subcommands (via `iq-cli-server` integration)

### `iq-cli-pro` (Enterprise)

Additions:
- `boot`  : start application lifecycle workflows (agentic reconciliation)
- `run`   : run script/resource (SPARQL and Groovy path)
- `trigger` : announce event to workflows
- `trust` : trust local/remote entities
- `models` : list model inventory

### `iq-cli-server` (Runtime manager)

Tree:
- `server`:
  - `api` (start/stop/reboot/status/health/dump/debug)
  - `mcp` (same as api but for MCP runtime)
  - `cluster` (list/add/remove nodes)

Example:
```bash
./bin/iq server api start
./bin/iq server api health
./bin/iq server api dump --path /tmp/iq-api-dump.tar.gz
./bin/iq server cluster list
``` 

## IPC and MCP

`iq-mcp` has an SPI-based builder under `MCPServerBuilder`:
- `McpServer.sync(provider).build()` to create MCP server
- tools registered by `MCPToolRegistry`
- pipeline ACL route via `ACLFilterMiddleware`

MCP tools available (cli-agnostic):
- `sparql-query` / `sparql-update` / `rdf-describe`
- `server-mgmt`
- `actor-trigger` bridge

## Authorization in camel routes

`IQRoutePolicy` controls access:
- reads HTTP principal from `CamelHttpServletRequest`
- checks `iq.route` identity plus policy in `acl/allow.sparql` script
- default allow when no policy script is found
- denies on missing principal or false policy

## Secrets management (TTL + CLI)

`iq-secrets` supports backends:
- local (`VFSPasswordVault` via `.iq/vault`)
- `hashicorp` Vault
- Azure Key Vault
- AWS Secrets Manager
- GCP Secret Manager

Env key:
- `IQ_SECRETS_BACKEND=local|hashicorp|aws|azure|gcp`

CLI commands (future/enterprise):
- `iq secret list`
- `iq secret get <key>`
- `iq secret set <key> <value>`
- `iq secret rotate <key>`
- `iq secret revoke <key>`
- `iq secret audit`

## TTL/graph schema and best-practices

### Base namespaces
```ttl
@prefix iq: <http://systems.symbol/iq#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
```

### Agent pipeline/action script store
```ttl
<iq:script:infer> a iq:SPARQLScript;
    rdfs:label "Infer labels";
    rdf:value """CONSTRUCT WHERE { ... }"""^^<urn:application/x-sparql-query> .
```

### Access control policy script example `acl/allow.sparql`
```ttl
<acl:allow> a <urn:application/x-sparql-query>;
    rdf:value """
PREFIX iq: <http://systems.symbol/iq#>
ASK {
  VALUES ?principal { <http://example.com/identity/admin> }
  ?principal a iq:AdminRole .
}"""^^<urn:application/x-sparql-query> .
```

### Model descriptor template
```ttl
<model:gpt-4> a iq:LLModel;
    skos:prefLabel "GPT-4";
    iq:endpoint "https://api.openai.com/v1/chat/completions";
    iq:provider "openai";
    iq:defaultTemperature 0.7 .
```

### Runtime group/cluster metadata
```ttl
<cluster:local> a iq:Cluster;
    rdfs:label "local cluster";
    iq:node <node:api/1>, <node:mcp/1>;
    iq:policy <acl:allow> .
```

## Core user commands with examples

### Initialize and status
```bash
./bin/iq init
./bin/iq status
./bin/iq list
```

### Run SPARQL from file
```bash
cat <<'SPARQL' > /tmp/exports.sparql
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?s ?name WHERE { ?s a foaf:Person; foaf:name ?name } LIMIT 20
SPARQL
./bin/iq sparql "$(cat /tmp/exports.sparql)"
```

### Backup and recover
```bash
./bin/iq backup
./bin/iq recover
```

### Agent trigger
```bash
./bin/iq trigger --event update-workflow
```

### Pro workflows
```bash
./bin/iq boot
./bin/iq run /tmp/recipe.sparql
./bin/iq trust me
```

### Server lifecycle
```bash
./bin/iq server api start
./bin/iq server api health
./bin/iq server api dump --path /tmp/iq-api-dump.tar.gz
./bin/iq server mcp status
./bin/iq server cluster add node1
./bin/iq server cluster list
```

## Additional templates and CLI helpers

### Create new route script entry (TTL scaffold)
```ttl
<scripts:route-update> a iq:SPARQLScript;
    rdfs:label "Update route mapping";
    rdf:value """INSERT DATA { ... }"""^^<urn:application/x-sparql-query> .
```

### Common CLI alias for local quick run
```bash
alias iqrun='IQ_HOME=~/iq-data ./bin/iq'
```

## Troubleshooting

- `Context not initialized`: run `iq init` and confirm `IQ_HOME` environment variable.
- `Missing HTTP principal`: check Camel request user principal or policy script in ACL.
- `MCP no mapper`: ensure `mcp-json-jackson2` is in classpath by `mvn dependency:tree`.
- `Secret backend not set`: set `IQ_SECRETS_BACKEND` and fill `.iq/vault` for local.

## Maintenance

- Use TTL + SPARQL config approach (no Java edits) for `iq` behavior updates.
- Add script under `src/main/resources/self/...` for modular features.
- Keep `iq-docs/man/` docs synchronized with command definitions and `iq-docs/todo`.
