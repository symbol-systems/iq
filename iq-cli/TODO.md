# iq-cli TODO — OSS surface

> **Status:** Draft — March 2026
> **Scope:** Analysis of gaps and planned features for the open-source CLI (`systems.symbol.CLI` / Picocli).
> **Basis:** `iq-kernel` SPEC (phases 1–3), inspired by `iq-mcp` tool registry and `iq-run-apis` controller patterns.

---

## 1. Current State

| Class | Picocli command | Implementation state |
|---|---|---|
| `AboutCommand` | `about` | Working — shows namespaces + self IRI |
| `BackupCommand` | `backup` | Working — timestamped TTL dump |
| `ExportCommand` | `export` | Working — `--to`, `--comment`, `--ns` options |
| `ImportCommand` | `import` | Working — `--from`, `--force` options |
| `InitCommand` | `init` | Working — `--from`, `--home`, `--store`, `--force` |
| `ListCommand` | `list` | Working — SELECT query via named IRI |
| `SPARQLCommand` | `sparql` | Partially working |
| `InferCommand` | `infer` | **Entire body commented out** |
| `ScriptCommand` | `script` | Unknown / likely stub |
| `RenderCommand` | `render` | Unknown / likely stub |
| `RecoverCommand` | `recover` | Unknown / likely stub |

**Structural problems:**

- `CLIContext.init()` re-implements workspace boot logic that duplicates `RealmPlatform.onStart()`. Contradicts kernel unification goal (SPEC §4.1).
- `AbstractCLICommand` has no connection to `AbstractKernelCommand` — these will diverge further without deliberate alignment.
- `CLIException extends Exception` is a parallel exception hierarchy alongside `KernelException`, `MCPException`, `OopsException`, etc. (SPEC §4.5).
- No pipeline / middleware: the CLI applies no auth, quota, or audit when executing commands. Every command calls the repository directly.
- No unit tests visible anywhere in `iq-cli/src/test/`.

---

## 2. Kernel Alignment Tasks

These are foundational; downstream feature work depends on them.

### 2.1 Wire `CLIContext` → `KernelBuilder` (SPEC Phase 1, step 4)

**Goal:** replace `CLIContext.init()` with `KernelBuilder`.

```java
// before
this.workspace = new Workspace(home);
this.repository = workspace.getCurrentRepository();

// after
KernelContext ctx = KernelBuilder.create()
    .withSelf(I_Self.self())
    .withSecrets(new EnvsAsSecrets())
    .withRealm(home)
    .build();
```

`CLIContext` becomes a thin Picocli-specific holder for a `KernelContext`, not a self-contained boot object. `Workspace` + `WorkspaceProbe` move to `iq-kernel.workspace` (SPEC §4.6), so `CLIContext` no longer needs to import them.

**Files:** `CLIContext.java`, `systems.symbol.CLI` (main entry point).

### 2.2 Extend `AbstractCLICommand` → `AbstractKernelCommand` (SPEC Phase 2, step 9)

**Goal:** bridge the Picocli `call()` method to the kernel command contract.

```java
public abstract class AbstractCLICommand
        extends AbstractKernelCommand
        implements Callable<Object> {

    @Override
    public final Object call() throws Exception {
        KernelRequest req = buildRequest();   // subclass fills params
        KernelResult<?> result = execute(req);
        return result.isOk() ? result.value() : handleError(result.error());
    }

    protected abstract KernelRequest buildRequest();
}
```

This removes the direct `CLIContext` field from every command; they receive it through `KernelContext` from the parent.

**Files:** `AbstractCLICommand.java`, all `*Command.java` files.

### 2.3 Drop `CLIException` → `KernelCommandException` (SPEC §4.5)

`CLIException extends Exception` is checked; `KernelCommandException extends KernelException extends RuntimeException`. Convert catch sites in `ListCommand` and `CLIContext` to use the kernel hierarchy. Keep a `@Deprecated` alias for one release cycle if needed.

**Files:** `CLIException.java`, `ListCommand.java`, `CLIContext.java`.

### 2.4 Add `I_AccessPolicy` guard (SPEC §4.2)

Wire a CLI-local `I_AccessPolicy` implementation that checks whether the workspace is initialised before allowing any command to proceed. Currently every command duplicates `if (!context.isInitialized()) throw ...`. A single middleware in a `SimplePipeline<KernelCallContext>` replaces all of these guards.

---

## 3. Feature Backlog — OSS Commands

### 3.1 `infer` — fix (currently commented out)

**What it should do:** run a named SPARQL `INSERT` / `CONSTRUCT` script stored in the realm graph and write inferred triples back into the repository.

**Approach:** resolve the script IRI via `IQScriptCatalog`, execute through `SPARQLExecutor` (in `iq-rdf4j`), log the number of added triples.

```
iq infer infer/index.sparql
iq infer --query <urn:iq:script:infer:core>
```

**Dependencies:** `iq-rdf4j` (SPARQLExecutor), `iq-platform` (IQScriptCatalog). No new COSS dependency.

### 3.2 `script` — fix (status unknown)

**What it should do:** load and execute a script file (SPARQL only in OSS; Groovy in pro). Return results as ASCII table or TTL.

```
iq script --file ./queries/find-actors.sparql
iq script --file ./queries/find-actors.sparql --format csv
```

**Approach:** read file, detect language from extension, delegate to `SPARQLExecutor` (SPARQL) or display "Groovy requires iq-cli-pro" for `.groovy` files. Output via `Display.java`.

**Dependencies:** `iq-rdf4j`. OSS-only: SPARQL files. Groovy boundary → pro.

### 3.3 `render` — fix (status unknown)

**What it should do:** apply a named SPARQL `CONSTRUCT` query and serialise the output graph in the requested RDF format.

```
iq render --template urn:iq:template:summary --format jsonld
iq render --template urn:iq:template:summary --format turtle
```

**Approach:** resolve template by IRI from the realm graph, execute CONSTRUCT, serialise with RDF4J `Rio`.

**Dependencies:** `iq-rdf4j`. Fully OSS.

### 3.4 `recover` — fix (status unknown)

**What it should do:** delete the current repository and restore from the most recent (or named) backup file.

```
iq recover
iq recover --from ./backups/2026-03-01/1741824000000.ttl
```

**Approach:** reuse `ImportExport.export/import` infrastructure already used by `BackupCommand` and `ImportCommand`. Prompt for confirmation unless `--force`.

**Dependencies:** `iq-rdf4j`. Fully OSS.

### 3.5 `status` — new command (inspired by `realm.status` MCP tool + `HealthCheckAPI`)

**What it should do:** print a health summary of the local workspace — no auth, no server.

**Proposed output:**

```
IQ status @ urn:iq:self:my-workspace
  store:       native (native-store)
  triples:     14,203
  namespaces:  8
  last backup: 2026-03-18 14:32 UTC  (./backups/2026-03-18/...)
  workspace:   /home/user/.iq  [OK]
```

**Dependencies:** `iq-kernel.workspace` (WorkspaceProbe — version, build date), `iq-rdf4j`. Fully OSS.

### 3.6 `validate` — new command (inspired by SHACL guard in `iq-mcp` `SparqlSafetyMiddleware`)

**What it should do:** run SHACL validation over the repository using shapes stored in the graph or a local file, and report violations as an ASCII table.

```
iq validate
iq validate --shapes ./shapes/core.ttl
iq validate --shapes urn:iq:shapes:platform
```

**Approach:** use RDF4J SHACL validator (`rdf4j-shacl`). Load shapes from the shapes graph if no `--shapes` flag is given.

**Dependencies:** `rdf4j-shacl`. Fully OSS.

### 3.7 `query` sub-commands — cleaner ergonomics

Wrap `SPARQLCommand` and `ListCommand` into a sub-command group with consistent output options:

```
iq query select "SELECT ?s WHERE { ?s a <urn:iq:Actor> }"
iq query construct "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o } LIMIT 10" --format turtle
iq query ask "ASK { <urn:iq:self> a <urn:iq:Workspace> }"   # exits 0/1
iq query describe <urn:iq:actor:myActor>
```

**Inspiration:** `sparql.query`, `sparql.update`, `rdf.describe`, `rdf.walk` MCP tool table (MCP_TOOLS.md).

**Dependencies:** `iq-rdf4j`. Fully OSS.

---

## 4. Cross-Cutting Concerns

### 4.1 Output formats

Introduce a `--format [table|ttl|jsonld|json|csv|nq]` global option (Picocli `@ParentCommand` mixin). Currently each command prints ad-hoc to `System.out`. Route all output through `Display.java` and add format-aware serialisers.

### 4.2 Exit codes

Map `KernelException` subtypes to meaningful POSIX exit codes via a top-level `ExceptionMapper`:

| Exception | Exit code |
|---|---|
| `KernelBootException` | 2 |
| `KernelAuthException` | 3 |
| `KernelCommandException` | 1 |
| `KernelSecretException` | 4 |
| `KernelBudgetException` | 5 |

### 4.3 Logging

Add `--verbose` / `--quiet` Picocli mixin. `--verbose` sets the SLF4J binding level to DEBUG. Currently the only log output is whatever SLF4J is bound to at startup.

---

## 5. Testing

No tests exist currently. Target coverage pattern mirrors `iq-mcp` tests:

| Test class | What it covers |
|---|---|
| `CLIContextTest` | KernelBuilder wiring, workspace init, close |
| `AboutCommandTest` | namespace display with in-memory RDF4J repository |
| `BackupCommandTest` | file written, TTL parseable |
| `ImportCommandTest` | import from fixture TTL, triple count matches |
| `QuerySelectCommandTest` | SELECT returns expected bindings |
| `QueryConstructCommandTest` | CONSTRUCT returns valid RDF graph |
| `QueryAskCommandTest` | ASK exit code 0 (true) / 1 (false) |
| `ValidateCommandTest` | SHACL violation detected from fixture |
| `StatusCommandTest` | ASCII summary parseable, no exceptions |

Use `SimpleEventHub` (from `iq-kernel`) and an in-memory RDF4J store for all unit tests. Integration tests (reading real `.iq/` workspace) excluded from default build: `-DskipITs=true`.

---

## 6. OSS / COSS Boundary Summary

| Capability | OSS (`iq-cli`) | COSS (`iq-cli-pro`) |
|---|---|---|
| Workspace boot (`init`, `about`, `status`) | ✅ | |
| SPARQL query / construct / ask / describe | ✅ | |
| TTL import / export / backup / recover | ✅ | |
| SHACL validation | ✅ | |
| SPARQL script execution | ✅ | |
| Groovy / Nashorn script execution | | ✅ |
| Agent workflow boot (`boot`) | | ✅ |
| Event trigger (`trigger`) | | ✅ |
| Trust / PKI (`trust`) | | ✅ |
| LLM model management (`model`) | | ✅ |
| Embedded server launch (`serve`) | | ✅ |
| Agent lifecycle management (`agent *`) | | ✅ |
| Realm admin (`realm *`) | | ✅ |

---

## 7. Implementation Order (suggested)

1. **Kernel alignment first** — items 2.1 → 2.2 → 2.3 → 2.4 (unblocks everything else)
2. **Fix broken commands** — `infer`, `script`, `render`, `recover` (quick wins, no new dependencies)
3. **Add `status`** (useful immediately, no complex deps)
4. **Add `query` sub-commands** (replaces `list` + `sparql` ergonomics)
5. **Add `validate`** (adds `rdf4j-shacl` dep, audit before merging)
6. **Output format mixin + exit codes** (polish, after core commands work)
7. **Unit test suite** (companion to each command implementation, not deferred to end)
