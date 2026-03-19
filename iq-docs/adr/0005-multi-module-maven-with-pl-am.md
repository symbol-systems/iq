# ADR 0005: Use `-pl <module> -am` for Efficient Builds

**Status:** Accepted

**Context:** Full `mvn install` on a multi-module repo is slow; changes usually affect only a subset.

**Decision:** Use Maven’s `-pl` with `-am` to build the target module and its dependencies for local work and CI targeted jobs.

**Consequences:**
- Speeds up iterative development and CI jobs by avoiding unnecessary module builds.
- Requires developers to understand module boundaries and dependency graph.
