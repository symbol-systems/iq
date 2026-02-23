# REVIEW_IQ-LAKE.md — Review & Migration Plan for `iq-lake`

**Status**: Module exists at `iq-lake/` but is currently *disabled* in the parent POM (commented out). The module contains a Quarkus-based pom with Tika, VFS and AWS S3 dependencies but minimal source/test content (some folders are empty). ✅ Restoration is feasible; the module appears to be a partially implemented (or archived) feature rather than intentionally deleted code.

---

## Quick Findings (2 sentences)
- The `iq-lake` module is a Quarkus/Tika-based ingestion and lake module that has its Quarkus plugin and some integrations commented out in `pom.xml`. This suggests past issues with plugin or dependency compatibility (Kotlin & Quarkus mix) and/or an incomplete implementation.

---

## Why it may have been disabled
- Quarkus plugin sections are commented out in the pom (sign of attempted or failed Quarkus upgrade).
- Code folders for `systems.symbol.io` are empty (no active implementations or moved code).
- Kotlin build plugin configured but sources are Java-only (possible mismatch/legacy build problems).

---

## Impact if left disabled
- Loss of an ingestion/`lake` capability in the main distribution (affects ingestion flows and demo pipelines).  
- Ongoing maintenance debt: commented build configuration and unused dependencies can cause confusion.

---

## Remediation Options (Mutually Exclusive)
1. **Restore & Harden (Recommended)** — Re-enable module in parent POM and fix build + tests. Best if the feature is still valuable.
2. **Extract & Archive** — Move the module to a separate repo (e.g., `iq-lake-old`) and add an `ARCHIVE.md` with migration notes.
3. **Remove** — Delete the module if functionality is deprecated and no longer needed (rare; only after confirmation).

---

## Recommended Action: Restore & Harden — Step-by-step
1. Repro: run `mvn -pl iq-lake -am clean install` and capture build errors (document them in a ticket).  
2. Align dependencies: ensure Quarkus and other plugin versions match parent pom (use `${quarkus.version}` and parent BOM).  
3. Restore Quarkus plugin usage (uncomment, update to current Quarkus version) or move non-Quarkus parts to a plain Java module.  
4. Resolve Kotlin settings: if Kotlin not used, remove Kotlin plugin and dependencies; if used, add Kotlin sources and validate jvmTarget.  
5. Add at least 1 unit test and 1 integration test (Tika ingestion basic scenario).  
6. Add CI job or extend existing workflow to include `-pl iq-lake -am` on PRs.  
7. Re-enable module in parent `pom.xml` (remove comment) and run full repository build.  
8. Add documentation: update `iq-lake/README.md` to describe purpose, configuration (S3 connection, VFS), and examples.

---

## Acceptance Criteria
- `mvn -pl iq-lake -am clean install` completes successfully on GitHub Actions and locally.  
- Module has a test that validates at least one ingestion path.  
- `README.md` documents usage and dependencies.  
- A follow-up ticket exists for performance and security hardening (e.g., S3 credentials management).

---

## Timeline & Effort Estimate
- Investigation & repro: 1–2 hours.  
- Build fixes & dependency alignment: 2–4 hours.  
- Tests + docs + CI update: 3–5 hours.  
- Total: 6–11 hours.

---

## If the team prefers Archive instead
- Move `iq-lake` to `archived/iq-lake` or a separate repo with tag `v0-archived`.  
- Add `ARCHIVE.md` describing why archived, how to reinstate, and notable files to review.

---

## Quick Checklist (PR Template)
- [ ] Ran `mvn -pl iq-lake -am clean install` locally.  
- [ ] Added unit + integration test(s).  
- [ ] Updated `README.md`.  
- [ ] Added CI check for this module.  

---

*Notes:* If the lake functionality has been replaced by other modules, consider the archive option; otherwise, restoration and hardening is recommended to preserve functionality.