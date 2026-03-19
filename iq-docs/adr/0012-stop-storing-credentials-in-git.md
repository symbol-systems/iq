# ADR 0012: Never Commit Credentials to Git

**Status:** Accepted

**Context:** Credentials accidentally committed have been observed in other projects; they are hard to rotate once in history.

**Decision:** Enforce that any secrets (API keys, tokens) reside outside the repository, in env vars or `.iq/vault`, and add a pre-commit check if possible.

**Consequences:**
- Reduces risk of leaked keys and compliance issues.
- Requires onboarding docs and tooling to make secret management easy for new contributors.
