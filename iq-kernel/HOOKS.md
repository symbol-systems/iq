# IQ Kernel Hooks + Event-Driven Triggers

## Objective
Add first-class, minimal-footprint event/Hook support across IQ layers so run-time operations in:
- RDF4J models and repositories
- script execution (e.g., `iq-run-apis`, `iq-platform` script engines)
- FSMs (e.g., `iq-agentic` / actor transitions)
- lake and storage operations
- CLI / MCP / REST surfaces

... can register and consume event-driven hooks with consistent semantics.

## Current baseline (already in repo)

`iq-kernel` already provides an event abstraction:
- `systems.symbol.kernel.event.I_EventHub` (publish/subscribe)
- `I_EventSink` (consume events)
- `KernelEvent` (topic, payload, source, timestamp)
- `SimpleEventHub` in-process implementation
- surface adapters (Camel/Vert.x) intended to provide richer `I_EventHub` implementations

`KernelBuilder` supports `withAttribute(key,value)` for surface-specific wiring (a minimal extension point for extra collaborators such as event hub, repository interceptors, etc.).

## Proposed hook architecture

### 1. Consistent core event pipeline
- Add a kernel-level `I_HookRegistry` or use existing `I_EventHub` for all events.
- Standardize event topics as IRIs in `systems.symbol.kernel.event.KernelTopics`.
- Use rich typed event payloads:
  - `KernelEvent` (existing)
  - marker interface `I_KernelEvent` for domain events (RDF, FSM, lake, script)

### 2. Kernel events for major engine phases
Define semantics and topics in `HOOKS.md` + code constants:
- `urn:iq:event:kernel:start`, `...:stop`, `...:restart`
- `urn:iq:event:kernel:command:pre`, `...:post`, `...:error`
- `urn:iq:event:kernel:workspace:open` etc.

### 3. RDF4J & repository hooks
Add wrapper layers/dispatch hooks in `iq-rdf4j` and/or `iq-platform` around model/repository operations via:
- `I_RepositoryEventSource` + `I_RepositoryEventListener` (interface) as thin adapter for `RepositoryConnection` methods.
- fire events around key calls: `add`, `remove`, `clear`, `commit`, `rollback`, `prepare`, `export`, `import`, `shutDown`.
- topics:
  - `urn:iq:event:rdf:statement:add`
  - `urn:iq:event:rdf:statement:remove`
  - `urn:iq:event:rdf:repository:commit`
  - `urn:iq:event:rdf:repository:rollback`

- event payload structure contains minimal info + optional SPARQL / IRI / model snippet.
- allow **quiet mode** (no-op) by injecting `NoopEventSink` with no listeners.

### 4. Scripts & execution hooks
- Add event emission around script lifecycle in `iq-platform` and `iq-run-apis`:
  - `urn:iq:event:script:load`, `...:compile`, `...:execute`, `...:complete`, `...:fail`
- payload: script name/IRI, user, parameters, duration, result, exception.
- integrate with existing script catalog classes.

### 5. FSM / actor hooks
- Add FSM-specific events in `iq-agentic` or `iq-kernel` generic:
  - `urn:iq:event:fsm:bookkeeping`, `...:transition:before`, `...:transition:after`, `...:transition:error`
- payload: state from/to, event name, actor id, bindings.

### 6. Lake / batch operations
- Events for lake lifecycle and data operations in `iq-lake`:
  - `urn:iq:event:lake:partition:load`, `...:store`, `...:delete`, `...:checkpoint`.
- event metadata include timestamp, source path, row count, status.

### 7. CLI + MCP trigger command integration
- Expose `I_EventHub` via `KernelContext` attribute bag (e.g., `ctx.get("eventHub")` or typed accessor).
- `iq-cli` command `trigger` and `run` should publish to topics:
  - `urn:iq:event:cli:command:start`, `...:complete`, `...:error`.
- `iq-cli-pro` `trigger` functionality already noted in TODO should leverage the same hub.

### 8. Deep hooks for custom extensions
- Hooks should be pluggable by topic pattern matching / wildcard (topic prefix).
- Provide `I_EventSink` adapter for scripts and triggers, e.g. `ExecScriptHook` or `SqlObserver`.
- `KernelBuilder` should allow `withHook(I_EventSink sink)` or `withAttribute("eventHub", hub)`.

### 9. Minimal footprint plan
- Keep `I_EventHub` in kernel as single dependency; avoid adding heavy new frameworks.
- In each module, add one wrapper/proxy class around the specific API:
  - RDF4J: `HookingRepositoryConnection` extends `RepositoryConnectionWrapper`
  - scripts: `HookingScriptExecutor` decorator
  - FSM: `HookingStateMachine` event deposit wrapper.

- As an initial migration, ensure if `I_EventHub` is absent, a fallback `SimpleEventHub` or `Noop` is used.
- Non-invasive: do not rewrite existing APIs; add events at boundary points for each major operation.

## Implementation roadmap (incremental)

1. Kernel core: define `KernelTopics`, `I_HookRegistry` optional, ensure `KernelBuilder` has API to wire an event hub and compose default `SimpleEventHub`.
2. CLI/REST/MCP surface: expose event hub in `KernelContext` and OTA start/stop events.
3. RDF4J: implement event wrapper around `RepositoryConnection` in `iq-rdf4j`, using injected `I_EventHub` from kernel context.
4. Script engine: emit before/after events in operations.
5. FSM: instrument transition edge in `iq-agentic` / script FSM to publish. Use a generic `I_FsmHook` to keep extraction.
6. Lake: instrument `iq-lake` connector operations with events.
7. Tests: for each event set, create unit tests around broker path (hub + listener) verifying delivery, payload, ordering.

## Event naming taxonomy (starting set)

- `urn:iq:event:kernel:...`
- `urn:iq:event:command:...`
- `urn:iq:event:rdf:...`
- `urn:iq:event:sparql:...`
- `urn:iq:event:script:...`
- `urn:iq:event:fsm:...`
- `urn:iq:event:lake:...`
- `urn:iq:event:cli:...`

## Sizing and safety
- Keep event payload small (metadata and references). Avoid sending complete `Model` for heavy operations; send IRI or summaries + on-demand retrieval.
- Allow `event.enable` configuration controls by namespace to avoid performance penalties.
- Guard hook execution paths with failure containment so event listener exceptions do not break core flows (central in `SimpleEventHub` already catches per-sink exceptions).

## Developer UX example

- A user configures `KernelBuilder.create().withAttribute("eventHub", customHub)`.
- `iq-rdf4j` auto-uses `I_EventHub` from `ctx.get("eventHub")` to send events for `add`, `remove` operations.
- A plugin subscribes to `urn:iq:event:rdf:statement:add` and triggers workflow in `iq-agentic`.

## Notes
- This doc is intentionally light on specific class names and risk-reward to fit design-first request.
- Real `HOOKS.md` references existing kernel event primitives as the minimum footprint baseline.
