# ADR 0020: Maintain Project Documentation in `iq-docs`

**Status:** Accepted

**Context:** Documentation scattered across modules leads to outdated guidance and duplication.

**Decision:** Author and update architecture, API and operational docs under `iq-docs/`, with module-specific docs linked from there.

**Consequences:**
- Single source of truth for docs and easier onboarding.
- Requires discipline to keep `README.md` pointers in sync with `iq-docs` content.
