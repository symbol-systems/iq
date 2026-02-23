# REVIEW_IQ-CLI.md — Review & Migration Plan for `iq-cli`

**Status**: `iq-cli/` exists, configured as a CLI (Picocli + shaded JAR) with a main class `systems.symbol.CLI`, but is disabled in the parent POM. The module appears usable and provides local tooling for developers and ops; it likely was disabled to reduce build time or because it required maintenance.

---

## Quick Findings (2 sentences)
- The CLI module is a mature artifact scaffold (shade plugin, Picocli, RDF4J runtime) and can produce a runnable jar; its README is minimal and lacks usage docs. This makes the CLI a good candidate for restoration as a dev-ops/utility tool provided reasonable tests and docs are added.

---

## Why it may have been disabled
- Build time / CI: CLI modules can inflate CI build time and were likely disabled to speed integration builds.  
- Maintenance: its README and examples are sparse (no usage or command docs).  
- Potential duplication: some features may have been superseded by new APIs.

---

## Impact if left disabled
- Developers and operators lose a convenient local tool for common tasks (e.g., repo inspection, migration helpers).  
- Hidden or outdated CLI may drift and break local dev workflows.

---

## Remediation Options (Mutually Exclusive)
1. **Restore & Document** (Recommended): Bring CLI back into repo with tests and usage docs.  
2. **Extract into a Tooling Repo**: move to `tools/iq-cli` with independent lifecycle and CI.  
3. **Keep Disabled & Archive**: add `ARCHIVE.md` and move code to an `archived/` area.

---

## Recommended Action: Restore & Document — Step-by-step
1. Repro: `mvn -pl iq-cli -am clean package` and verify the shaded jar can be executed: `java -jar iq-cli.jar --help`.  
2. Add usage documentation: a `USAGE.md` with examples for common commands and expected env variables.  
3. Add unit tests for command handlers and an integration smoke test that runs `--help` and a simple command.  
4. Add a small GitHub Actions job that builds and publishes the CLI artifact (optional) on tags.  
5. Add a short `cli` section to the main README that describes the tooling and when to use it.  
6. Decide on distribution: keep as repo artifact or publish to GitHub Packages for convenience.

---

## Acceptance Criteria
- `mvn -pl iq-cli -am clean package` completes in CI and locally.  
- `java -jar target/iq-cli.jar --help` prints usage and exit code 0 in a test.  
- README + `USAGE.md` documents common commands and examples.  
- CI optionally packages/publishes the CLI artifact on release.

---

## Timeline & Effort Estimate
- Investigation & build repro: 1–2 hours.  
- Tests + docs + CI: 3–5 hours.  
- Total: 4–7 hours.

---

## Quick Checklist (PR Template)
- [ ] Run `mvn -pl iq-cli -am clean package` locally.  
- [ ] Add unit + integration tests for commands.  
- [ ] Add `USAGE.md` and update top-level README.  
- [ ] Add CI job and re-enable module in parent POM (or extract to tooling repo).

---

*Note:* If CI time is a concern, use a gated restore: enable the module but add it to an optional CI matrix or a weekly build cadence.