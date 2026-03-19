# ADR 0014: Enforce Static Analysis via Checkstyle/SpotBugs

**Status:** Accepted

**Context:** Inconsistent code style and undetected bugs slow down code review and increase technical debt.

**Decision:** Run Checkstyle and SpotBugs in CI; configure them to catch common mistakes relevant to this codebase (nulls, resource leaks, thread safety).

**Consequences:**
- Maintains code quality and prevents regressions.
- Developers need to run these tools locally or accept failing CI checks.
