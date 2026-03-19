# ADR 0013: Require Java 21 for Build and Runtime

**Status:** Accepted

**Context:** The project uses Quarkus 3.x and language features that depend on Java 21.

**Decision:** Enforce Java 21 in CI and local builds using Maven toolchains or `maven.compiler.release`.

**Consequences:**
- Ensures consistent builds across developers and CI.
- Developers on older JDKs must install Java 21; use the provided `mvnw` wrapper to avoid version mismatch.
