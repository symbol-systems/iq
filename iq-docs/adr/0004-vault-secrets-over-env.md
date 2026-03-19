# ADR 0004: Prefer Vault-backed Secrets over Plain Env Vars

**Status:** Accepted

**Context:** Secrets used in CI and production should not be baked into environment snapshots or checked in.

**Decision:** Store secrets in `.iq/vault` (VFSPasswordVault) and fall back to env vars only when vault entry is missing.

**Consequences:**
- Developers can keep a local `.iq/vault` copy for dev without exposing keys in repo.
- Build pipelines need to populate vault or set env vars explicitly.
