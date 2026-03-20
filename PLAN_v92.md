# PLAN_v92: CLI / API / MCP convergence and refactor

## 1. Verdict

Yes — a small intentional refactor is required to make the CLI upgrade plan robust, non-duplicative, and maintainable.

- `iq-platform` should own shared command runtime and operational APIs.
- `iq-apis` should expose API runtime lifecycle endpoints for `start/stop/health/dump`.
- `iq-mcp` should expose MCP tool wrappers for the same operations.
- `iq-cli` / `iq-cli-pro` / `iq-cli-server` should be orchestration layers on top of platform services (not each implementing separate lifecycle logic).
- `CLI_UPGRADE.md` is approved as the strategic vision; `PLAN_v92.md` is the tactical execution path.

## 2. Refactor objectives

1. Extract common CLI base classes from `iq-cli` into `iq-platform`:
   - `AbstractCLICommand` -> `platform/cli/AbstractCommand`
   - `CLIContext` -> `platform/cli/CLIContext`
   - shared arg-parsing decorators/hooks
2. Implement `ServerRuntimeManager` in `iq-platform`:
   - methods: start(), stop(), reboot(), health(), debug(), dump()
   - support both local process and remote control plane via `I_RuntimeControl` SPI
3. Add `iq-apis` lifecycle endpoints and internal `RuntimeService`:
   - `/api/runtime/start`, `/stop`, `/reboot`, `/health`, `/dump`, `/debug`
   - auto-select `api` vs `mcp` runtime based on query/path
4. Add `iq-mcp` tools to expose `server` operations via MCP layer:
   - `mcp:/tools/server/start`, etc. Returning `MCPResult` status object
5. Create new module `iq-cli-server` (or promote from existing in future):
   - command tree mirrors `server` subcommands from plan
   - calls `ServerRuntimeManager` via `iq-platform` services
   - supports `server api ...` and `server mcp ...`
6. Ensure existing `iq-cli`/`iq-cli-pro` continue to work and can use the same `ServerRuntimeManager` extension.

## 3. Mapping to CLI_UPGRADE.md

- Keep the matrix exactly as in `CLI_UPGRADE`:
  - `iq-cli`: OSS features
  - `iq-cli-pro`: enterprise plus advanced script/auth
  - `iq-cli-server`: full runtime control and health management
- Reuse `connect`, `model`, `script`, `agent` paths from existing CLI commands; don’t copy into `iq-cli-server`, reference shared implementations instead.

## 4. Implementation phases (terse)

Phase 1: Baseline Cleanup (1 day)
- add/verify tests for `iq-cli`, `iq-cli-pro` commands.
- extract shared CLI packages to `iq-platform`.

Phase 2: Shared runtime API (2 days)
- add `ServerRuntimeManager` to `iq-platform`
- add common `RuntimeHealthService` and `RuntimeDumpService` in platform.

Phase 3: run-apis/mcp surface (2 days)
- implement endpoint bindings in `iq-apis`
- add mcp tools in `iq-mcp` for same ops

Phase 4: iq-cli-server (3 days)
- new module + commands
- integration tests using local fake runtime and process mocks

Phase 5: QA + docs (2 days)
- update `README`/`SPEC` and `CLI_UPGRADE.md` reference, smoke tests.

## 5. Required repo changes

- `pom.xml` add module `iq-cli-server`
- `iq-platform/src/main/java/...` add CLI/Runtime interfaces
- `iq-apis` add controller classes in `systems.symbol.platform.runtime`
- `iq-mcp` add wrappers in `systems.symbol.mcp.tool.server`
- `iq-cli-pro` extend from platform class and wire pro-only features.

## 6. Rolling out without breakage

- preserve `iq` binary alias for all variants.
- keep existing commands in OSS stable while additive for pro and server.
- add `IQ_EDITION=(community|pro|server)` environment switch.
- keep `iq-cli` minor checks that reject server-only commands with clear warnings.

## 7. Validation criteria

- command compatibility matrix passes with unit tests.
- one integration test for lifecycle sequence: start->health->debug->dump->stop.
- API endpoints in `iq-apis` return 200 + diagnostics payload.
- MCP tools can invoke same runtime service and produce `MCPResult`.
