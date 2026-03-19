# ADR 0016: Build Container Images Using `mvnw` Wrapper

**Status:** Accepted

**Context:** The project uses Maven and builds in different environments; relying on system Maven leads to inconsistent builds.

**Decision:** Use `./mvnw` in container image build steps (`./bin/build-image`) to guarantee Maven version consistency.

**Consequences:**
- Builds are reproducible across CI and developer machines.
- The wrapper adds ~3MB to the repo but avoids “works on my machine” issues.
