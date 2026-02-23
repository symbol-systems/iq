# TODO_CLAUDE.md — Actionable Improvements for IQ Codebase

## PRIORITY: CRITICAL (Security/Stability)

### SECURITY-002: Remove Secrets Dump Side Effect
**File(s)**: `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java` (lines ~52-58)  
**Issue**: `RDFDump.dump()` writes full model to `debug.ttl` on LLM config errors (security risk, side effect)  
**Action**:
1. Remove the `debug.ttl` dump
2. Log error details to structured audit log instead (include model size, missing config fields, but not secrets)
3. Add test case: verify error handling without side effect

**Impact**: Prevents accidental credential exposure; improves error logging  
**Effort**: 1 hour

---

### ERRORS-001: Remove XXX/XXXX Placeholder Methods
**File(s)**: 
- `iq-trusted/src/main/java/systems/symbol/platform/TrustedPlatform.java` (lines ~71, ~89, ~90, ~129, ~143, ~148)
- `iq-trusted/src/main/java/systems/symbol/MY_IQ_AI.java` (lines ~44-45)  

### STABILITY-001: Fix Static Initialization Error Handling
**File(s)**: `iq-run-apis/src/main/java/systems/symbol/platform/RealmPlatform.java` (lines ~47-51)  
**Issue**: Static block initializes `RealmManager` which throws checked exception; JVM will crash silently on init failure  
**Action**:
1. Replace static initialization with Quarkus `@Observes StartupEvent` handler (move RealmManager init to `onStart()` method)
2. Add explicit error handling: log error, set state flag, optionally fail fast
3. Ensure `getTasks()` checks if threads were initialized before use

**Impact**: Prevents cryptic startup failures  
**Effort**: 1.5 hours

---

## PRIORITY: HIGH (Functionality & Observability)

### OBSERVABILITY-001: Add Quarkus Health Checks
**File(s)**: New file(s) to create  
**Issue**: No `/q/health` endpoint; no way to check if realms, LLM providers are ready  
**Action**:
1. Add SmallRye Health dependency to pom.xml (if not present)
2. Implement custom `HealthCheck` for each realm (check repository connection, vault accessibility)
3. Implement custom `HealthCheck` for LLM provider (test connectivity if not real cost)
4. Expose readiness/liveness probes for Kubernetes (if applicable)

**Impact**: Enables health-based routing and monitoring  
**Effort**: 2-3 hours

---

### ERRORS-002: Standardize Error Handling — Use Optional or Result<T>
**File(s)**: 
- `iq-platform/src/main/java/systems/symbol/realm/RealmManager.java` (returns null on error)
- `iq-platform/src/main/java/systems/symbol/llm/gpt/LLMFactory.java` (returns null on error)
- Other factory/retrieval methods  
**Issue**: Mix of exceptions and null returns; callers must check for null (NPE risk)  
**Action**:
1. Create `Result<T>` type or use `Optional<T>` consistently:
   ```java
   public Optional<GPTWrapper> llm(Resource self, Model model, I_Secrets secrets) throws SecretsException
   ```
2. Audit callers: replace `if (result == null)` with `if (result.isEmpty())`
3. Add `@NotNull` / `@Nullable` annotations (use JetBrains or Jakarta annotations)
4. Add test case: verify error paths return empty/fail appropriately

**Impact**: Prevents NPE; clarifies error handling intent  
**Effort**: 3-4 hours (refactor + tests)

---

### DOCUMENTATION-001: Add API Contract Documentation
**File(s)**: All REST controller classes (e.g., `ChatAPI.java`, `ModelAPI.java`, `TokenAPI.java`)  
**Issue**: No JavaDoc on endpoints; inputs/outputs/errors not documented  
**Action**:
1. For each REST endpoint, add JavaDoc with:
   - Description of what endpoint does
   - Parameter descriptions (required/optional)
   - Possible error responses (with error codes)
   - Example request/response (or reference to `/docs` example)
2. Use OpenAPI/Swagger annotations if available (e.g., `@Operation`, `@ApiResponse`)
3. Example:
   ```java
   /**
* Retrieves LLM response for a given query.
* @param namedMap LLM configuration name (required)
* @param repo Repository name (required)
* @param promptPath Path to prompt template (required)
* @param query User query (required)
* @return LLM response wrapped in JSON
* @throws OopsException if repo/config not found or LLM call fails
*/
   @GET
   public Response llm(...)
   ```

**Impact**: Improves developer experience; reduces support burden  
**Effort**: 2-3 hours (documentation)

---

### DOCUMENTATION-002: Add Production Deployment Guide
**Files**: New file `DEPLOYMENT.md`  
**Issue**: No guidance on secrets setup, HTTPS, scaling, backup procedures  
**Action**:
1. Document environment variables needed (LLM keys, vault location, etc.)
2. Document HTTPS setup (certificate paths, Quarkus config)
3. Document scaling (multi-realm setup, repository replication if applicable)
4. Document backup procedures (`.iq/repositories` backup, vault backup)
5. Troubleshooting section (common startup errors, debug tips)

**Impact**: Enables confident production deployment  
**Effort**: 2-3 hours (documentation)

---

### LOGGING-001: Add Structured Logging for Key Operations
**File(s)**: Controllers, factories, managers (start with `RealmPlatform`, `LLMFactory`)  
**Issue**: Narrative logs are hard to parse at scale; no structured fields for alerting  
**Action**:
1. Add structured logging library (e.g., Logstash Logback encoder or SLF4J MDC)
2. For key operations (startup, LLM call, realm load), log structured JSON:
   ```json
   {
 "timestamp": "2026-02-01T10:00:00Z",
 "level": "INFO",
 "event": "llm_call_start",
 "realm": "iq:test",
 "model": "gpt-3.5-turbo",
 "tokens": 2048,
 "trace_id": "abc123"
   }
   ```
3. Add correlation IDs to request context (for tracing)

**Impact**: Enables production observability and alerting  
**Effort**: 3-4 hours (implementation + test)

---

## PRIORITY: MEDIUM (Quality & Architecture)

### TESTING-001: Add Concurrency Tests for ThreadManager
**File(s)**: `iq-platform/src/test/java/systems/symbol/platform/ThreadManagerTest.java` (new file)  
**Issue**: ThreadManager manages concurrent tasks but no concurrency tests visible  
**Action**:
1. Create test case: spawn multiple tasks, verify they run concurrently and complete
2. Create test case: verify task exception doesn't crash other tasks
3. Create test case: verify stop() waits for all threads to terminate

**Impact**: Improves confidence in concurrent correctness  
**Effort**: 2 hours

---

### TESTING-002: Add Coverage for Error Paths
**File(s)**: Test classes across modules  
**Issue**: Error handling code not obviously tested  
**Action**:
1. Add test cases for null/missing resources (e.g., missing realm, missing LLM config)
2. Add test cases for invalid inputs (e.g., malformed IRIs, invalid SPARQL)
3. Add test cases for timeout/connectivity issues (mock if necessary)
4. Configure code coverage reporting in pom.xml (e.g., JaCoCo)

**Impact**: Improves reliability; catches regressions  
**Effort**: 3-4 hours (test writing)

---

### RDF-001: Add SHACL Validation for RDF Models
**File(s)**: New file(s) under `iq-rdf4j/` or `iq-platform/`  
**Issue**: RDF schemas are implicit; no validation on load  
**Action**:
1. Define SHACL shapes for key entities (Realms, Agents, Workflows) in `.ttl` files
2. At realm/model load time, (validate against SHACL shapes)
3. Log/report validation errors
4. Document SHACL constraints in schema documentation

**Impact**: Catches schema drift early; improves data quality  
**Effort**: 4-5 hours (SHACL design + implementation)

---

### LLM-001: Add Retry Logic & Circuit Breaker
**File(s)**: `iq-platform/src/main/java/systems/symbol/llm/gpt/GPTWrapper.java` (or new wrapper)  
**Issue**: No retry on transient failures; no circuit breaker for cascading failures  
**Action**:
1. Add exponential backoff retry logic (e.g., 3 retries, 1s → 2s → 4s delays)
2. Implement simple circuit breaker (track failures, skip calls if threshold exceeded)
3. Make retry count and delay configurable
4. Add metrics (retry count, circuit breaker state)

**Impact**: Improves resilience to transient LLM failures  
**Effort**: 2-3 hours

---

### ARCH-001: Migrate to Explicit Dependency Injection
**File(s)**: `iq-run-apis`, `iq-platform` (incremental)  
**Issue**: Static singletons and manual initialization increase coupling  
**Action**:
1. Add Quarkus CDI beans for key singletons (RealmManager, ThreadManager, LLMFactory)
2. Use constructor injection in controllers and services (instead of static access)
3. Document migration path (can be done incrementally)

**Impact**: Reduces coupling, improves testability  
**Effort**: 6-8 hours (refactoring + tests)

---

### ARCH-002: Remediate Disabled Modules
**File(s)**: `pom.xml`, `BUILD.md`  
**Issue**: 8 modules commented out (iq-lake, iq-finder, iq-cli, etc.); produce a migration guide first  
---

### CODE-001: Fix Commented Code
**File(s)**:
- `iq-run-apis/src/main/java/systems/symbol/platform/OpenApiCustomFilter.java` (auth disabled)
- `iq-run-apis/src/main/java/systems/symbol/platform/RealmPlatform.java` (commented config)  

**Action**:
1. Review each commented-out block
2. If outdated: delete it
3. If incomplete feature: create GitHub issue, reference in commit message
4. Add pre-commit hook (optional) to warn on commented code

**Impact**: Improves code clarity  
**Effort**: 1 hour

---

## PRIORITY: LOW (Nice-to-Have, Long-Term)

### OBSERVABILITY-002: Add Metrics & Tracing
**Issue**: No metrics on SPARQL query performance, LLM call latency, repository size  
**Action**:
1. Add Micrometer (Quarkus has built-in support)
2. Instrument key operations: SPARQL queries, LLM calls, repository access
3. Expose Prometheus metrics at `/q/metrics`

**Impact**: Enables performance monitoring and optimization  
**Effort**: 3-4 hours

---

### DOCUMENTATION-003: Add Architecture Diagrams
**Issue**: No visual representation of module relationships, data flow  
**Action**:
1. Create module dependency diagram (using tool like PlantUML or draw.io)
2. Create request flow diagram (client → API → RDF → LLM)
3. Create trust zone diagram
4. Add to `ARCHITECTURE.md`

**Impact**: Improves onboarding for new developers  
**Effort**: 2-3 hours

---

### DOCUMENTATION-004: Add Migration Guide for Schema Changes
**Issue**: No guide for updating TTL schemas without breaking running systems  
**Action**:
1. Document versioning strategy for TTL/SPARQL
2. Create example migration script
3. Document backward compatibility expectations

**Impact**: Enables safe evolution of schemas  
**Effort**: 1-2 hours

---

### DATABASE-001: Add Connection Pooling for RDF Repositories
**Issue**: No visible pooling; connections opened per-request  
**Action**:
1. Evaluate connection pooling options (e.g., HikariCP for JDBC, custom pool for RDF4J)
2. Implement pooling if load tests show contention
3. Document pool sizing guidelines

**Impact**: Improves performance under load  
**Effort**: 3-4 hours (if needed)

---

### LLM-002: Add A/B Testing Framework
**Issue**: No mechanism for testing multiple prompts/models  
**Action**:
1. Design configuration for A/B test groups
2. Implement routing based on test group
3. Log results for analysis

**Impact**: Enables optimization of prompts/models  
**Effort**: 4-5 hours

---

### CI-001: Add Security Scanning to CI/CD
**Issue**: No SAST, DAST, or dependency scanning visible  
**Action**:
1. Add Dependabot or Snyk for dependency scanning
2. Add SAST tool (e.g., SonarQube, Checkmarx)
3. Configure pipeline to fail on high-severity issues

**Impact**: Improves security posture  
**Effort**: 2-3 hours (setup)

---

### CI-002: Add Performance Benchmarks
**Issue**: No performance baseline or regression detection  
**Action**:
1. Create benchmarks for key operations (SPARQL query, LLM call, realm load)
2. Run in CI; report results; fail if regression detected

**Impact**: Prevents performance regressions  
**Effort**: 3-4 hours

---

## PRIORITY: TECHNICAL DEBT TRACKING

### TD-001: LLMFactory Error Recovery
**Status**: Identified (SECURITY-002, ERRORS-002)  
**Linked Issues**: Security-002, Errors-002  
**Action**: Consolidate and execute in next sprint

### TD-002: Auth Filter Finalization
**Status**: Identified (SECURITY-001)  
**Linked Issues**: Security-001, Stability-001  
**Action**: Unblock before production

### TD-003: Error Handling Standardization
**Status**: Identified (ERRORS-001, ERRORS-002)  
**Linked Issues**: Errors-001, Errors-002, Testing-002  
**Action**: High impact, medium effort; recommend for next sprint

### TD-004: Documentation Gaps
**Status**: Identified (DOCUMENTATION-001, DOCUMENTATION-002)  
**Linked Issues**: Documentation-001, Documentation-002, Documentation-003  
**Action**: Spread across sprints or dedicate documentation sprint

---

## ROADMAP (Recommended Sequence)

### Sprint 1 (Week 1-2): Security & Stability Hardening
- SECURITY-001: Enable auth filter
- SECURITY-002: Remove secrets dump
- STABILITY-001: Fix static initialization
- ERRORS-001: Remove/clarify XXX/XXXX
- **Outcome**: Production-ready security/startup

### Sprint 2 (Week 3-4): Observability & Error Handling
- OBSERVABILITY-001: Add health checks
- ERRORS-002: Standardize error handling
- LOGGING-001: Add structured logging
- **Outcome**: Production observability

### Sprint 3 (Week 5-6): Testing & Quality
- TESTING-001: Concurrency tests
- TESTING-002: Error path coverage
- CODE-001: Remove dead code
- **Outcome**: Improved test coverage

### Sprint 4+: Long-Term Improvements
- ARCH-001: Dependency injection migration
- RDF-001: SHACL validation
- LLM-001: Retry/circuit breaker
- DOCUMENTATION-*: Guides and diagrams
- **Outcome**: Scalability and maintainability

---

## SUMMARY: IMPACT & EFFORT MATRIX

| Task | Impact | Effort | Priority | Owner |
|------|--------|--------|----------|-------|
| SECURITY-001 | Critical | 4h | CRITICAL | Platform Team |
| SECURITY-002 | High | 1h | CRITICAL | Platform Team |
| ERRORS-001 | High | 2h | CRITICAL | Code Owner |
| STABILITY-001 | High | 1.5h | CRITICAL | Platform Team |
| OBSERVABILITY-001 | High | 3h | HIGH | Platform Team |
| ERRORS-002 | High | 4h | HIGH | Architecture |
| DOCUMENTATION-001 | Medium | 3h | HIGH | API Owner |
| DOCUMENTATION-002 | High | 3h | HIGH | DevOps/Product |
| LOGGING-001 | Medium | 4h | HIGH | Platform Team |
| TESTING-001 | Medium | 2h | MEDIUM | QA |
| TESTING-002 | Medium | 4h | MEDIUM | QA |
| RDF-001 | Medium | 5h | MEDIUM | RDF Owner |
| LLM-001 | Medium | 3h | MEDIUM | LLM Owner |
| ARCH-001 | High | 8h | MEDIUM | Architecture |
| ARCH-002 | Low | 3h | MEDIUM | Maintenance |
| CODE-001 | Low | 1h | MEDIUM | Maintenance |
| **CRITICAL TOTAL** | **—** | **9h** | **—** | **—** |
| **HIGH TOTAL** | **—** | **17h** | **—** | **—** |
| **MEDIUM TOTAL** | **—** | **30h** | **—** | **—** |

---

## CONCLUSION

**IQ is ready for incremental hardening and scaling.** Execute CRITICAL tasks (9h) immediately to unblock production. Allocate HIGH priority tasks (17h) to next 2 sprints. Use MEDIUM tasks (30h) to build long-term quality and scalability. The system has solid foundations; these improvements consolidate and extend them.

