# ADR Index (0000)

This file lists all Architecture Decision Records for the repository.

## ADRs

- [0001: Use Quarkus Dev Mode for Local Iteration](0001-use-quarkus-dev-mode.md)
- [0002: RDF-First Domain Modeling](0002-rdf-first-domain-model.md)
- [0003: LLM Providers Configured as Named Maps](0003-llm-providers-config-as-map.md)
- [0004: Prefer Vault-backed Secrets over Plain Env Vars](0004-vault-secrets-over-env.md)
- [0005: Use `-pl <module> -am` for Efficient Builds](0005-multi-module-maven-with-pl-am.md)
- [0006: Store TTL and SPARQL as Classpath Resources](0006-ttl-sparql-resources-in-classpath.md)
- [0007: Use Structured Logging (SLF4J) over System.out](0007-avoid-runtime-stdout-logging.md)
- [0008: Prefer Immutable Domain Objects](0008-immutable-domain-objects.md)
- [0009: Use Quarkus REST Client for External APIs](0009-use-quarkus-rest-client.md)
- [0010: Unit Tests Use In-Memory RDF4J Repository](0010-test-with-inmemory-rdf.md)
- [0011: Mock LLM Providers in Unit Tests](0011-llm-mocks-in-unit-tests.md)
- [0012: Never Commit Credentials to Git](0012-stop-storing-credentials-in-git.md)
- [0013: Require Java 21 for Build and Runtime](0013-require-java-21.md)
- [0014: Enforce Static Analysis via Checkstyle/SpotBugs](0014-checkstyle-and-spotbugs.md)
- [0015: Declare Public APIs via OpenAPI Specs](0015-declare-apis-via-openapi.md)
- [0016: Build Container Images Using `mvnw` Wrapper](0016-container-builds-use-mvnw.md)
- [0017: Namespaced Configuration Key Conventions](0017-namespaced-configuration-keys.md)
- [0018: Parameterize SPARQL Using Predefined Variables](0018-safe-sparql-parameterization.md)
- [0019: Use Testcontainers for Integration Tests](0019-integration-tests-use-testcontainers.md)
- [0020: Maintain Project Documentation in `iq-docs`](0020-docs-in-iq-docs.md)
