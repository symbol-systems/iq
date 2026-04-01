# Contributing to IQ

Thank you for your interest in contributing to **IQ** (systems.symbol), a scalable multi-agent orchestration platform powered by RDF graphs, LLM reasoning, and knowledge synthesis.

## Quick Start

### Prerequisites

- **Java 21 LTS** (JDK) — download from [eclipse-temurin.adoptium.net](https://adoptium.net)
- **Maven 3.8.x+** — provided via `./mvnw` (Maven Wrapper)
- **Git** with Git LFS (for large test fixtures)
- **Docker** (optional, for containerized builds/tests)

### Cloning & Setup

```bash
git clone https://github.com/symbol-systems/iq.git
cd iq
export JAVA_HOME=/path/to/jdk-21  # Only if not on PATH
./mvnw clean verify -DskipITs=true               # Full build (5-10 min)
```

The project uses **Maven Wrapper** (`./mvnw`/`./mvnw.cmd`); do not invoke `mvn` directly.

---

## Essential Commands

### Building

| Command | Purpose | Time |
|---------|---------|------|
| `./mvnw clean install` | Full clean build, all modules | 10–15 min |
| `./mvnw -pl <module> -am compile` | Incremental compile + dependencies | 1–3 min |
| `./mvnw -DskipTests -DskipITs clean install` | **Fastest rebuild** (skip tests) | 5–8 min |

**Example:** Compile `iq-apis` and its dependencies:
```bash
./mvnw -pl iq-apis -am compile
```

### Testing

| Command | Purpose |
|---------|---------|
| `./mvnw test` | Run unit tests (default, fast) |
| `./mvnw -DskipITs=false verify` | Unit + integration tests (slow) |
| `./mvnw -pl <module> -am test` | Test single module + deps |
| `./mvnw test -Dtest=MyClassTest#myMethod` | Test single method |

**Example:** Test `iq-mcp` only:
```bash
./mvnw -pl iq-mcp test
```

### Development & Debugging

| Command | Purpose |
|---------|---------|
| `./mvnw -pl iq-apis -am quarkus:dev` | **Hot-reload dev server** on `http://localhost:8080` |
| `./bin/iq` | Run CLI against built artifacts |
| `./bin/iq-cli server mcp start` | Start MCP server (headless) |
| `./bin/compile-apis` | Quick compile-only for iq-apis |

**Hot-reload workflow:**
```bash
./mvnw -pl iq-apis -am quarkus:dev &    # Terminal 1: starts dev server
# Edit source, save → auto-recompile + reload
# Access http://localhost:8080/q/dev for Quarkus console
```

### Docker & Container Builds

```bash
./bin/build-image                        # Build Docker image (requires Docker)
docker run -p 8080:8080 symbol/iq:0.94.0  # Run image
```

### Other Utilities

```bash
./bin/inspect                           # Report codebase metrics
./bin/log-clean                         # Clear accumulated logs
./bin/release                           # Prepare release (maintainers only)
```

---

## Project Structure

| Path | Purpose |
|------|---------|
| `iq-apis/` | Quarkus REST API + LLM endpoints (runtime entry point) |
| `iq-platform/` | Core business logic, LLM wrappers, RDF orchestration |
| `iq-rdf4j/`, `iq-rdf4j-graphs/` | RDF4J repository drivers, SPARQL tooling |
| `iq-mcp/` | Model Context Protocol (MCP) implementation |
| `iq-cli/`, `iq-cli-pro/`, `iq-cli-server/` | Command-line interfaces |
| `iq-connect/*` | Connector library (AWS, Azure, Slack, GitHub, etc.) — see [iq-connect/STATUS.md](iq-connect/STATUS.md) |
| `iq-trusted/`, `iq-secrets/` | Security, JWT, vault integration |
| `iq-agentic/` | Multi-agent orchestration & reasoning |
| `iq-docs/` | Architecture decisions (ADRs), documentation |

**Build model:** 33 Maven modules, centralized dependency versions in root `pom.xml`.

---

## Development Workflow

### 1. **Before Starting**

- Ensure tests pass: `./mvnw test` (iq-mcp, iq-platform modules minimum)
- Check the [CLAUDE_TODO.md](todo/CLAUDE_TODO.md) for known issues and recommendations

### 2. **Making Changes**

- **Single module focus:** Use `-pl <module> -am` to compile only what you changed
- **Commit early & often:** Encourage atomic, well-described commits
- **RDF-first logic:** New features usually add `.ttl` / `.sparql` files plus a Java hook
  - Script catalog: `iq-platform/src/main/resources/iq/model/`
  - Hook entry point: `iq-platform/src/main/java/systems/symbol/platform/`

### 3. **Testing Your Changes**

```bash
# Compile + unit test single module
./mvnw -pl iq-<mymodule> -am clean test -DskipITs=true

# If adding integration tests, enable them:
./mvnw -pl iq-<mymodule> -am clean test -DskipITs=false

# Run hot-reload dev server to verify behavior
./mvnw -pl iq-apis -am quarkus:dev
```

### 4. **Local Debugging**

- **Logs:** Located in `logs/` directory; check `logs/current.log`
- **Quarkus dev UI:** `http://localhost:8080/q/dev` (when running `quarkus:dev`)
- **MCP testing:** Use `./bin/curl_api` or `./bin/curl_mcp` for quick endpoint tests
- **Java debugger:** Attach IDE debugger to Quarkus dev process (auto-enabled on port 5005)

### 5. **Submitting Changes**

- **Keep PRs focused:** One feature/fix per PR; avoid mixture of unrelated changes
- **Document intent:** Describe *why* in commit messages and PR body, not just *what*
- **Update `.ttl`/`.sparql` catalogs:** If adding model logic, update related script catalogs
- **No secrets in code:** Use environment variables and `.iq/vault/` for sensitive config

---

## Code Style & Conventions

### Java & Logging

- **Logging framework:** SLF4J + Logback (never `System.out.println` for production logs)
- **Example:**
  ```java
  private static final Logger log = LoggerFactory.getLogger(MyClass.class);
  
  public void doSomething() {
      log.info("Starting operation");
      try {
          // ...
      } catch (Exception e) {
          log.error("Operation failed", e);
          throw new PlatformException("details...", e);
      }
  }
  ```

- **Format:** Consistent with existing files; use IDE auto-formatter if available

### Groovy & Scripts

- **DSL scripts:** Use `.groovy.ttl` / `.sparql` files in resource directories
- **No embedded SQL/SPARQL strings:** Externalize to catalog files

### Testing

- **Unit tests:** Colocate in `src/test/java`, file pattern `*Test.java`
- **Integration tests:** File pattern `*IT.java`, run with `-DskipITs=false`
- **Mocking:** Use Mockito for external dependencies; avoid real API calls in unit tests

---

## Common Issues & Troubleshooting

| Issue | Solution |
|-------|----------|
| `Could not resolve dependency` | Run `./mvnw clean install` on root; check Maven repo connectivity |
| `var cannot be resolved to a type` | Ensure Java 21 compiler is active; check `iq-platform/pom.xml` `<release>` setting |
| Quarkus dev server won't start | Check port 8080 is free; kill existing Java process or use `-Dquarkus.http.port=9090` |
| LLM endpoint returns 401 Unauthorized | Check `MY_JWT_SECRET` env var is set; validate `.iq/vault/` secrets |
| Tests pass locally but fail in CI | Clear Maven cache: `rm -rf ~/.m2/repository/systems/symbol/`; rebuild |

---

## Architecture & Design Docs

- **System overview:** [README.md](README.md)
- **Copilot instructions (comprehensive):** [.github/copilot-instructions.md](.github/copilot-instructions.md)
- **Connector status & roadmap:** [iq-connect/STATUS.md](iq-connect/STATUS.md)
- **Known issues & recommendations:** [todo/CLAUDE_TODO.md](todo/CLAUDE_TODO.md)
- **API docs:** [iq-apis/docs/](iq-apis/docs/)

---

## Getting Help

- **Documentation:** Check [iq-docs/](iq-docs/), [README.md](README.md), and module `README.md` files
- **Issues & questions:** Open a GitHub Issue with:
  - Clear title and reproduction steps
  - Output of `./bin/inspect` (high-level metrics)
  - Relevant log snippets from `logs/current.log`
- **PRs & reviews:** Mention `@symbol-systems` maintainers for code review

---

## Release Process (Maintainers)

```bash
./bin/release             # Prepares release artifacts
# Follow prompts for version bump, changelog, etc.
git push origin main --tags
```

---

## License

Contributions to IQ are licensed under the same terms as the project. By contributing, you agree that your contributions will be licensed accordingly.

---

**Happy contributing!** 🚀

For detailed system architecture, see [.github/copilot-instructions.md](.github/copilot-instructions.md).

