# IQ CLI Upgrade Plan

## Purpose

Document the roadmap and design for CLI packages:
- `iq-cli` (Open Source)
- `iq-cli-pro` (Commercial)
- `iq-cli-server` (Commercial SaaS/On-Prem)

Goals:
1. Ensure good coverage and clear feature ownership across editions.
2. Keep functionality DRY and reusable.
3. Introduce `iq-cli-server` as runtime orchestration manager for API/MCP.
4. Establish a practical implementation and test plan.

---

## 1. Edition responsibilities

### 1.1 iq-cli (OSS)

Core functionality (fresh install anyone can use):
- Persistent knowledge graph init/verify (`init`, `verify`, `status`).
- Connector discovery and basic orchestration (`connector list`, `connector run`).
- Model import/export (Turtle/JSON-LD) and local SPARQL execution.
- `script` and `agent` surface for local playbook and simple transition testing.
- Configuration and platform health readouts.

Prohibitions for OSS:
- `groovy` script execution in `script` command (already stubbed).
- Managed yield clusters or distributed node control.
- Locked down authority operations (`host start/stop/reboot/debug`).

### 1.2 iq-cli-pro (paid)

Includes everything from `iq-cli` + extensions for enterprise operators:
- Workflow stage orchestration (`flow`, `pipeline` commands).
- Advanced connectors + audit mode.
- Snapshot management and ingest/extract for governance.
- Parallelized/transactional command sets and CLI-driven Scheduled imports.
- `model` and `connector` deep lifecycle operations.
- Output policies (JSON/YAML for non-human automation).
- `api` and `mcp` lightweight remote control hooks (no direct process manager) - connected to server. 

### 1.3 iq-cli-server (paid SaaS/on-prem)

New dedicated runtime lifecycle manager, not in OSS.
Core features:
- `iq-server start` / `iq-server stop` / `iq-server reboot` (graceful and forced).
- `iq-server status` / `iq-server health` / `iq-server dump`.
- `iq-server debug` (toggle debug logging into runtime, access runtime process details, memory snapshot). 
- `iq-server version` / `iq-server config` / `iq-server audit`.
- `iq-server api` / `iq-server mcp` subcommands with scoped operations and connection test.
- Multi-instance management (`iq-server list`, `iq-server cluster add/remove` for federated/cluster control).

SaaS/on-prem control plane requirements:
- Authentication/authorization guard + RBAC.
- Agent-based or native process supervisor per node.
- Support for persistent state in `.iq` / Docker/ContainerApps.
- Remote CLI endpoint plus local discovery (e.g., gRPC or HTTP API on controller).

---

## 2. Feature matrix (minimum)

Feature | iq-cli | iq-cli-pro | iq-cli-server
---|---|---|---
connector list/run | ✓ | ✓ | ✓
model import/export | ✓ | ✓ | ✓
script SPARQL | ✓ | ✓ | ✓
script Groovy | ✗ | ✓ | ✓
agent run/trigger | ✓ (lite) | ✓ (full) | ✓ (full)
start/stop/reboot/debug/dump/health | ✗ | partial hooks | ✓
API/MCP runtime management | ✗ | remote access | ✓ (native process manager)
audit/log streaming | basic | advanced | centralized + access logs
configuration management | local | multi-env | multi-tenant with secrets

---

## 3. Design and DRY principles

1. Shared core engine should live in `iq-platform` / `iq-kernel`.
2. CLI command definitions are thin wrappers that call common helpers in platform libs.
3. Use `AbstractCLICommand` / `CLIContext` baseline provided by `iq-cli` and enhance in Pro/Server.
4. Add `ServerCommand` hierarchy in `iq-cli-server` that reuses existing `ConnectorCommand` and `ModelCommand` logic:
   - `ServerLifecycleCommand` -> common start/stop/reboot workflow.
   - `ServerHealthCommand` -> uses `HealthService` from `iq-platform`.
   - `ServerDebugCommand` -> uses `DiagnosticsService`.
5. Keep CLI arg parsing and help docs consistent with CmdLineProfile.
6. Add integration tests in `iq-cli-pro` and `iq-cli-server` with shared `TestCLIExecutor` harness.

---

## 4. API/MCP runtime management model

`iq-cli-server` will control at least two runtime categories:
- `api` (HTTP server for IQ run APIs)
- `mcp` (model control plane / background orchestration workers)

Subcommand pattern:
- `iq server api start` / `iq server mcp start`
- `iq server api stop` / `iq server mcp stop`
- `iq server api reboot` / `iq server mcp reboot`
- `iq server api debug --enable` / `--disable`
- `iq server api dump --path /tmp/dump.tar.gz` / `--format json`
- `iq server api health --verbose` / `--timeout`.

Implementation notes:
- Abstract common `ServerRuntimeManager` interface in `iq-platform`.
- Provide wrappers for local process (Java `ProcessHandle`) and remote HTTP-based control endpoints.
- On SaaS, any `start` may create container/VM via `azd`/`k8s` operators (as future feature).

---

## 5. Delivery roadmap

1. Baseline cleanup (week 1)
   - Audit existing `iq-cli` commands, remove stale stubs.
   - Extract shared utilities from `iq-cli/core` into `iq-platform/cli`.
2. Pro upgrade (week 2)
   - Add missing enterprise Connector/Model commands.
   - Add expanded tests under `iq-cli-pro/src/test`.
3. Server MVP (week 3)
   - Create new module `iq-cli-server` in repo root.
   - Implement lifecycle commands using shared platform services.
   - Add health/debug/dump subcommands.
4. Validation (week 4)
   - Integration tests with fake API/MCP runtime.
   - Run full `mvn -pl iq-cli,iq-cli-pro,iq-cli-server test` or `verify`.
5. Docs and release
   - Update `iq-docs/docs/GUIDE.md` with server usage.
   - Publish CLI docs and release note.

---

## 6. Testing strategy

- `iq-cli`: unit + small integration tests around command parsing and in-memory repository.
- `iq-cli-pro`: include security, connector workflows, and SPARQL/Groovy tests.
- `iq-cli-server`: runtime state tests (start/stop/reboot), failover, health checks, and dump verification.
- Add CLI-level e2e script (`bin/cli-server-smoke`) to validate runtime lifecycle.

---

## 7. Migration notes

- Existing `iq-cli` users can continue using old command sets.
- Pro users should get automatic `server` prefix commands where appropriate.
- For enterprise, enable `IQ_CLI_EDITION=server` environment at runtime.
- Keep backward compatibility with `iq` command alias to reduce breakage.

---

## 8. Success metrics

- 95% command coverage in unit tests across 3 modules.
- 2+ enterprise use cases for `iq-server lifecycle` managed from CLI.
- Minimal duplicate code in shared `iq-platform` helpers.
- Documented API for new `ServerRuntimeManager` and clear extension points.
