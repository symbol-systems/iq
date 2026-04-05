# IQ Test Infrastructure Analysis - April 2026

## Executive Summary

The IQ codebase has **foundational test infrastructure** in place but significant gaps in **connector mocking and test fixtures**. Currently, 69 unit tests pass in `iq-apis`, but 20+ connector integration tests **skip silently in CI** due to missing cloud service credentials. The solution is to implement mock-based test fixtures using OkHttp MockWebServer (already in dependencies).

---

## 1. Existing Test Structure

### iq-apis Test Organization
| Component | Files | Status | Notes |
|-----------|-------|--------|-------|
| OAuth | 2 (.java) | ✅ **WORKING** | OAuthTestInitializer auto-registers test clients |
| Platform Smoke | 1 (.java) | ✅ **PASSING** | 69 tests validating endpoint availability |
| SPARQL/FedX | 3 (.java) | ⚠️ **BASIC** | Minimal coverage, no data fixtures |
| RDF | 2 (.java) | ⚠️ **BASIC** | Query validation only |
| Trust/Tokens | 1 (.java) | ⚠️ **BASIC** | Token validation tests |
| **Total** | **9 files** | | All use `@QuarkusTest` annotation |

### iq-connect Test Organization
| Module | Test Files | Status | Gap |
|--------|-----------|--------|-----|
| AWS | 3 (unit, scanner, modeller) | ❌ IT Skips | No AWS mock credentials in CI |
| Azure | 1 (unit test) | ❌ IT Skips | No Azure mock |
| GCP | 1 (unit test) | ❌ IT Skips | No GCP mock |
| Snowflake | 1 (unit test) | ❌ IT Skips | No Snowflake mock |
| Slack | 1 (unit test) | ❌ IT Skips | No Slack mock |
| GitHub | 2 (unit + modeller) | ❌ IT Skips | No GitHub mock |
| Others (16 modules) | 1-2 each | ❌ IT Skips | No mocks |
| **Total** | **~28 files** | | Only basic lifecycle tests |

### Test File Structure Example
```
iq-connect-aws/
└── src/test/java/systems/symbol/connect/aws/
├── AwsConnectorTest.java  [unit: no credentials needed]
├── AwsConnectorIntegrationTest.java   [uses Assumptions.assumeTrue on env vars]
├── AwsConnectorScannerCompositionTest.java
├── AwsModellerTest.java
└── NO MOCK FIXTURES   [MISSING]
```

---

## 2. Test Dependencies Currently Available

### Core Testing Framework
✅ **Already in Dependencies** (from root pom.xml):
- **JUnit 5**: junit-jupiter-api, junit-jupiter-engine (v5.14.3)
- **Quarkus**: quarkus-junit5 (test scope)
- **REST**: rest-assured (v5.3.0) — fluent HTTP testing API
- **Mocking HTTP**: okhttp mockwebserver (v4.12.0) — **KEY for mocking!**
- **Mocking Objects**: mockito-core, mockito-junit-jupiter (selective modules)
- **Serialization**: Jackson (core, databind, annotations) — JSON/YAML
- **RDF**: rdf4j-model, rdf4j-model-api, rdf4j-query (v5.3.0-M2)

❌ **NOT Currently Used**:
- WireMock — HTTP mocking with request matching
- Testcontainers — Docker-based testing (PostgreSQL, MySQL, etc.)
- Spring Test — Not applicable (using Quarkus instead)
- REST Assured OAuth extension — Available but not used

### Test Configuration Files
- `iq-apis/src/test/resources/application-test.properties` — Test OAuth config
- `iq-apis/src/test/resources/application-test.yml` — Quarkus test config
- Default `skipITs=true` in root pom.xml

---

## 3. Test Initializers & Infrastructure

### ✅ OAuthTestInitializer (WORKING)
**Location**: `iq-apis/src/test/java/systems/symbol/controller/platform/OAuthTestInitializer.java`

**How it works**:
```java
@ApplicationScoped
public class OAuthTestInitializer {
@Inject OAuthAuthorizationServer authServer;

void onStart(@Observes StartupEvent startup) {
// Automatically registers test clients when tests start
registry.register(new OAuthClient("cli", "secret", ...));
registry.register(new OAuthClient("test-client", "secret", ...));
}
}
```

**Status**: ✅ Picked up from test classpath, activates automatically, all tests pass

### ❌ RDFTestInitializer (MISSING)
**Needed for**: SPARQLQueryValidatorTest, FedXResourceTest, custom SPARQL tests

**What it should do**:
- Initialize in-memory RDF4J repository
- Load sample graph data (FOAF, SKOS, domain-specific)
- Register SPARQL queries as fixtures
- Provide repository injection to tests

### ❌ MCPTestInitializer (MISSING)
**Needed for**: Tool testing, resource testing, prompt testing

**What it should do**:
- Register sample MCP tools during startup
- Register sample resources and schemas
- Provide mock tool execution handlers
- Setup prompt templates for testing

### ✅ OAuthTestResource (WORKING)
**Location**: `iq-apis/src/test/java/systems/symbol/controller/platform/OAuthTestResource.java`

Complements OAuthTestInitializer by providing REST endpoint helpers (likely for token management in tests)

---

## 4. Connector Credential Requirements

### AWS Connector
- **SDK**: AWS SDK for Java v2.20.28 (S3, STS, EC2, IAM, CloudTrail, Config, Pricing)
- **Required ENV vars**: 
  - `AWS_ACCESS_KEY_ID` 
  - `AWS_SECRET_ACCESS_KEY`
  - `AWS_REGION` (optional, defaults to "us-east-1")
- **Test Pattern**: `AwsConnectorIntegrationTest` uses `Assumptions.assumeTrue(System.getenv(...) != null, "...")`
- **CI Impact**: Skips silently in GitHub Actions (no credentials provided)

### Azure Connector
- **SDK**: Azure Identity + Azure Resource Manager v2.26.0
- **Required ENV vars**: 
  - `AZURE_SUBSCRIPTION_ID`
  - `AZURE_TENANT_ID` (optionally)
  - `AZURE_CLIENT_ID` & `AZURE_CLIENT_SECRET` (service principal auth)
- **Test Pattern**: Similar assumeTrue pattern needed
- **CI Impact**: Currently would skip

### GCP Connector
- **SDK**: Google Cloud Resource Manager v1.66.0
- **Required ENV vars**:
  - `GOOGLE_CLOUD_PROJECT`
  - `GOOGLE_APPLICATION_CREDENTIALS` (path to service account JSON)
- **Test Pattern**: Similar assumeTrue pattern needed
- **CI Impact**: Currently would skip

### Snowflake Connector
- **SDK**: Minimal dependencies (likely JDBC + Snowflake JDBC driver to be added)
- **Required ENV vars**:
  - `SNOWFLAKE_ACCOUNT` (e.g., "xy12345.us-east-1")
  - `SNOWFLAKE_USER`
  - `SNOWFLAKE_PASSWORD`
  - `SNOWFLAKE_DATABASE` (optional)
  - `SNOWFLAKE_SCHEMA` (optional)
- **Test Pattern**: Similar assumeTrue pattern needed
- **CI Impact**: Currently would skip

### Other Connectors (by category)

**Cloud Infrastructure** (3):
- GCP (covered above)
- DigitalOcean: `DIGITALOCEAN_API_TOKEN`
- Docker: `DOCKER_HOST` (local or remote daemon)

**Communication** (4):
- Slack: `SLACK_BOT_TOKEN` or `SLACK_APP_TOKEN`
- GitHub: `GITHUB_TOKEN` or `GITHUB_PERSONAL_ACCESS_TOKEN`
- Office 365: OAuth token + Microsoft Graph
- Google Apps: `GOOGLE_OAUTH_TOKEN` for Gmail, Calendar, Drive

**SaaS Platforms** (3):
- Salesforce: `SALESFORCE_CLIENT_ID`, `SALESFORCE_CLIENT_SECRET`, `SALESFORCE_USERNAME`, `SALESFORCE_PASSWORD`
- Stripe: `STRIPE_API_KEY` (secret key)
- Confluence: `CONFLUENCE_DOMAIN`, `CONFLUENCE_EMAIL`, `CONFLUENCE_API_TOKEN`

**Data Warehouses** (3):
- Snowflake (covered above)
- Databricks: `DATABRICKS_WORKSPACE_URL`, `DATABRICKS_PERSONAL_ACCESS_TOKEN`
- Datadog: `DATADOG_API_KEY`, `DATADOG_APP_KEY`

**Kubernetes & Container** (1):
- K8s: `KUBECONFIG` path or in-cluster config

**Data Formats** (1):
- Parquet: File path-based (no external credentials)

**Template** (1):
- Boilerplate for custom connectors

---

## 5. CI/CD Context

### GitHub Actions Workflows

#### `tests.yml` — Unit Tests Only
```yaml
- Run: mvn --batch-mode -DskipITs test
- JDK: Java 21
- Env vars: NONE provided for connectors
- Result: All integration tests skip gracefully via Assumptions.assumeTrue()
```

#### `jars.yaml` — Build & Package
```yaml
- Run: mvn --batch-mode test package -DskipITs
- Uses: Git LFS for large model files
- Publish: To GitHub Packages
- Integration tests: Skipped (skipITs=true)
```

#### `docker.yaml` — Container Image
```yaml
- Run: mvn -pl iq-apis -am package -Dquarkus.container-image.build=true -DskipITs
- Publish: To ghcr.io
- Integration tests: Skipped
```

### Current Skip Pattern
```java
// Used in AwsConnectorIntegrationTest
Assumptions.assumeTrue(
System.getenv("AWS_ACCESS_KEY_ID") != null && 
System.getenv("AWS_SECRET_ACCESS_KEY") != null,
"AWS credentials are required for integration test"
);
```

**Behavior**: Test doesn't run → no failure, no warning → CI passes ✓ But no validation

**Problem**: Tests never run in CI, so regressions aren't caught

---

## 6. Current Test Patterns

### Pattern 1: Unit Tests (No External Dependencies)
```java
// iq-connect-aws/src/test/java/systems/symbol/connect/aws/AwsConnectorTest.java
public class AwsConnectorTest {
@Test
void testRefreshFailsWithoutApiKey() {
AwsConnectorConfig config = new AwsConnectorConfig(null, Duration.ofMinutes(5), null);
AwsConnector connector = new AwsConnector("urn:iq:connector:aws", config);

try { connector.refresh(); }
catch (Exception e) { /* expected */ }

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
}

@Test
void testKernelStartStop() {
AwsConnector connector = new AwsConnector("urn:iq:connector:aws", config);
AwsConnectorKernel kernel = new AwsConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || 
   connector.getStatus() == ConnectorStatus.SYNCING || 
   connector.getStatus() == ConnectorStatus.ERROR);
}
}
```

**Status**: ✅ Works, always passes, runs in CI

---

### Pattern 2: Quarkus Integration Tests (HTTP-based)
```java
// iq-apis/src/test/java/systems/symbol/controller/platform/PlatformSmokeTest.java
@QuarkusTest
@DisplayName("IQ Platform Smoke Tests")
public class PlatformSmokeTest {

@Test
@DisplayName("MCP: GET /mcp/health — Health check should succeed")
void testMcpHealth() {
given()
.when()
.get("/mcp/health")
.then()
.statusCode(anyOf(equalTo(200), equalTo(503), equalTo(401)));
}
}
```

**Status**: ✅ Works, 69 tests pass, runs in CI

**Framework**: RestAssured + Hamcrest matchers + @QuarkusTest lifecycle

---

### Pattern 3: Conditional Integration Tests (Environment Variable Gating)
```java
// iq-connect-aws/src/test/java/systems/symbol/connect/aws/AwsConnectorIntegrationTest.java
public class AwsConnectorIntegrationTest {

@Test
void testAwsConnectorRefreshWithRealCredentials() {
Assumptions.assumeTrue(
System.getenv("AWS_ACCESS_KEY_ID") != null && 
System.getenv("AWS_SECRET_ACCESS_KEY") != null,
"AWS credentials are required for integration test"
);

AwsConfig config = new AwsConfig(System.getenv("AWS_REGION") ?? "us-east-1");
AwsConnector connector = new AwsConnector("urn:aws:connector:test", config);

connector.refresh();
assertTrue(connector.getStatus() != null);
}
}
```

**Status**: ⚠️ Pattern correct, but no mocks → skipped in CI, runs only with real credentials

**Problem**: Can't test in CI without exposing secrets

---

### Pattern 4: RDF Model Testing (Existing)
```java
// iq-connect-core/src/test/java/systems/symbol/connect/core/ConnectorGraphModellerTest.java
public class ConnectorGraphModellerTest {

@Test
void testGraphAwareEntityAndOntologyWrites() {
Model model = new LinkedHashModel();
IRI graph = SimpleValueFactory.getInstance().createIRI("urn:test:graph");

TestModeller modeller = new TestModeller(model, graph, ...);
IRI subject = modeller.createUser("john.doe@example.org");

assertEquals("urn:test:user:john.doe%40example.org", subject.stringValue());
assertTrue(model.contains(subject, rdfType(), ...));
}
}
```

**Status**: ✅ Works, no external dependencies, uses in-memory RDF4J

---

## 7. Missing Test Infrastructure

### Gap 1: Connector Mock Fixtures
| Connector | Mock Status | Library | Solution |
|-----------|------------|---------|----------|
| AWS | ❌ Missing | AWS SDK HTTP | OkHttp MockWebServer |
| Azure | ❌ Missing | Azure REST | OkHttp MockWebServer |
| GCP | ❌ Missing | Google Cloud REST | OkHttp MockWebServer |
| Snowflake | ❌ Missing | JDBC or REST | Testcontainers or OkHttp |
| Slack | ❌ Missing | Slack API (HTTP) | OkHttp MockWebServer |
| GitHub | ❌ Missing | GitHub API (HTTP) | OkHttp MockWebServer |
| All others | ❌ Missing | Various | Case-by-case |

### Gap 2: RDF Test Infrastructure
- ❌ No `RDFTestInitializer` to auto-setup test repositories
- ❌ No shared sample graphs (FOAF, SKOS, domain data)
- ❌ No fixtures for SPARQL query testing
- ❌ No test data builders for RDF models

### Gap 3: MCP Test Infrastructure
- ❌ No `MCPTestInitializer` for tool/resource registration
- ❌ No mock tool implementations
- ❌ No mock resource fixtures
- ❌ No prompt template testing

### Gap 4: Shared Test Utilities
- ❌ No base class for connector tests (CredentialFixture, MockServiceBuilder)
- ❌ No test data builders
- ❌ No common assertions for connectors
- ❌ No environment variable default handlers

### Gap 5: CI/CD Integration
- ❌ No mock-based integration tests in GitHub Actions
- ❌ No way to validate connector code without real credentials
- ❌ No test failure detection for broken connectors

---

## 8. Recommended Architecture

### 8.1 Shared Test Utilities (NEW)
**Location**: `iq-connect/iq-connect-core/src/test/java/systems/symbol/connect/fixtures/`

```java
// ConnectorTestBase.java
public abstract class ConnectorTestBase {
protected MockServiceServer mockServer = new MockServiceServer();
protected CredentialFixture credentials = new CredentialFixture();

@BeforeEach
void setupMocks() { mockServer.start(); }

@AfterEach
void teardownMocks() { mockServer.stop(); }
}

// CredentialFixture.java
public class CredentialFixture {
public static String mockAWSAccessKey() { return "AKIAIOSFODNN7EXAMPLE"; }
public static String mockAWSSecretKey() { return "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"; }
// ... etc for all services
}

// MockServiceBuilder.java
public class MockServiceBuilder {
public MockResponse awsS3ListBuckets() { 
return new MockResponse()
.setResponseCode(200)
.setHeader("content-type", "application/xml")
.setBody("<ListBucketsResult>...</ListBucketsResult>");
}
// ... other AWS responses
}
```

### 8.2 Per-Connector Mock Tests (NEW)
**Location**: `iq-connect/iq-connect-aws/src/test/java/systems/symbol/connect/aws/mocks/`

```java
// MockAwsServiceServer.java
public class MockAwsServiceServer extends ConnectorTestBase {
@BeforeEach
void setupAwsMocks() {
mockServer.enqueueResponse("/", awsS3ListBuckets());
mockServer.enqueueResponse("/iam/", awsIAMListUsers());
mockServer.enqueueResponse("/ec2/", awsEC2DescribeInstances());
}
}

// AwsConnectorMockIT.java
public class AwsConnectorMockIT extends MockAwsServiceServer {
@Test
void testRefreshWithMockAWS() {
String mockEndpoint = mockServer.getUrl().toString();

AwsConnectorConfig config = new AwsConnectorConfig(
mockAWSAccessKey(),
mockAWSSecretKey(),
"us-east-1",
mockEndpoint  // Use mock endpoint
);

AwsConnector connector = new AwsConnector("urn:iq:connector:aws", config);
connector.refresh();

assertTrue(connector.getStatus() == ConnectorStatus.IDLE || 
   connector.getStatus() == ConnectorStatus.SYNCING);
}
}
```

### 8.3 RDF Test Infrastructure (NEW)
**Location**: `iq-apis/src/test/java/systems/symbol/rdf/fixtures/`

```java
// RDFTestInitializer.java
@ApplicationScoped
public class RDFTestInitializer {
@Inject Repository repository;

void onStart(@Observes StartupEvent startup) {
loadSampleGraphs();
registerSPARQLQueries();
}
}

// SampleGraphsFixture.java
public class SampleGraphsFixture {
public static Model foafGraph() {
Model model = new LinkedHashModel();
// Create FOAF vocabulary graph
return model;
}

public static Model skosGraph() { /* ... */ }
public static Model domainGraph() { /* ... */ }
}
```

### 8.4 MCP Test Infrastructure (NEW)
**Location**: `iq-apis/src/test/java/systems/symbol/mcp/fixtures/`

```java
// MCPTestInitializer.java
@ApplicationScoped
public class MCPTestInitializer {
@Inject MCPServer mcpServer;

void onStart(@Observes StartupEvent startup) {
registerTestTools();
registerTestResources();
registerTestPrompts();
}
}

// MockToolFixture.java
public class MockToolFixture {
public static Tool echoTool() {
return new ToolBuilder()
.name("echo")
.description("Echo input")
.addInputSchema("message", "string")
.build();
}
}
```

---

## 9. Implementation Roadmap

### Phase 1: Shared Test Utilities (Week 1)
- [ ] Create `iq-connect-core/src/test/java/systems/symbol/connect/fixtures/`
- [ ] Implement `ConnectorTestBase.java`
- [ ] Implement `CredentialFixture.java` (mock creds for all services)
- [ ] Implement `MockServiceBuilder.java` (MockWebServer setup helper)
- [ ] Add OkHttp MockWebServer dependency (if needed — already present)
- **Effort**: 1-2 days
- **Validation**: All extends ConnectorTestBase successfully

### Phase 2: AWS + Azure Mock Tests (Week 1-2)
- [ ] Create AWS mock fixture responses
- [ ] Create `iq-connect-aws/src/test/java/systems/symbol/connect/aws/mocks/`
- [ ] Implement `AwsConnectorMockIT.java` with MockWebServer
- [ ] Repeat for Azure, GCP, Snowflake
- **Effort**: 3-4 days
- **Validation**: 4 mock-based IT tests run in CI without credentials

### Phase 3: RDF Test Infrastructure (Week 2-3)
- [ ] Create `RDFTestInitializer.java`
- [ ] Create `SampleGraphsFixture.java` with FOAF/SKOS/domain data
- [ ] Add fixtures to `iq-apis/src/test/resources/`
- [ ] Update SPARQLQueryValidatorTest to use fixtures
- **Effort**: 2-3 days
- **Validation**: SPARQLQueryValidatorTest uses fixture data

### Phase 4: MCP Test Infrastructure (Week 3)
- [ ] Create `MCPTestInitializer.java`
- [ ] Create mock tool/resource fixtures
- [ ] Extend PlatformSmokeTest to validate MCP structures
- **Effort**: 1-2 days
- **Validation**: MCP endpoints return valid tool/resource schemas

### Phase 5: CI/CD Integration (Week 4)
- [ ] Update GitHub Actions to run mock-based IT tests
- [ ] Add missing connectors (Slack, GitHub, Salesforce, etc.)
- [ ] Document test patterns for future maintainers
- **Effort**: 1-2 days
- **Validation**: CI runs 20+ connector IT tests successfully

### Phase 6: Testcontainers (Optional, Week 4+)
- [ ] Add Testcontainers for database connectors (if any)
- [ ] Add Testcontainers for Snowflake/DataDog (if REST approach insufficient)
- **Effort**: 2-3 days (per service)
- **Validation**: Realistic service behavior in tests

---

## 10. Quick Start: How to Create a Connector Mock

### Step 1: Create Mock Fixture Responses
```java
// iq-connect-aws/src/test/java/systems/symbol/connect/aws/fixtures/AwsMockFixture.java
public class AwsMockFixture {

public static MockResponse s3ListBuckets() {
return new MockResponse()
.setResponseCode(200)
.setHeader("Date", "Mon, 05 Apr 2026 10:00:00 GMT")
.setBody("""
<ListBucketsResult>
  <Buckets>
<Bucket>
  <Name>test-bucket-1</Name>
  <CreationDate>2026-04-01</CreationDate>
</Bucket>
  </Buckets>
</ListBucketsResult>
""");
}
}
```

### Step 2: Extend ConnectorTestBase
```java
// iq-connect-aws/src/test/java/systems/symbol/connect/aws/AwsConnectorMockIT.java
public class AwsConnectorMockIT extends ConnectorTestBase {

private MockServiceServer mockAwsServer;
private AwsConnector connector;

@BeforeEach
void setup() {
mockAwsServer = new MockServiceServer();
mockAwsServer.enqueue(AwsMockFixture.s3ListBuckets());

AwsConnectorConfig config = new AwsConnectorConfig(
"MOCK_KEY",
"MOCK_SECRET",
"us-east-1",
mockAwsServer.getUrl().toString() // Override endpoint!
);
connector = new AwsConnector("urn:iq:connector:aws", config);
}

@Test
void testRefreshWithMockAwsS3() {
connector.refresh();
assertEquals(ConnectorStatus.IDLE, connector.getStatus());
}

@AfterEach
void cleanup() {
mockAwsServer.shutdown();
}
}
```

### Step 3: Run Test
```bash
mvn -pl iq-connect/iq-connect-aws test
# AwsConnectorMockIT.java runs WITHOUT AWS credentials!
```

---

## 11. Summary: Current vs. Future State

| Aspect | Current | Future |
|--------|---------|--------|
| **iq-apis Tests** | 69 passing (OAuth, Platform, SPARQL, Trust) | +30 (RDF fixtures, MCP tests) |
| **Connector Unit Tests** | ~28 files, basic lifecycle | Same + infrastructure |
| **Connector Mock Tests** | ❌ 0 files | ✅ 28 files (one per connector) |
| **CI/CD Coverage** | Unit only, IT skipped | Unit + Mock IT running |
| **RDF Testing** | Minimal | Full with fixtures, sample data |
| **MCP Testing** | Basic endpoint checks | Full tool/resource validation |
| **Test Infrastructure** | OAuthTestInitializer | + RDFTestInitializer + MCPTestInitializer |
| **Mocking Libraries** | OkHttp MockWebServer (available) | Used actively |
| **CI Validation** | Passes but incomplete | Comprehensive regressions caught |

---

## 12. Files to Create (Summary)

### Core Infrastructure
1. `iq-connect-core/src/test/java/systems/symbol/connect/fixtures/ConnectorTestBase.java`
2. `iq-connect-core/src/test/java/systems/symbol/connect/fixtures/CredentialFixture.java`
3. `iq-connect-core/src/test/java/systems/symbol/connect/fixtures/MockServiceBuilder.java`
4. `iq-apis/src/test/java/systems/symbol/rdf/RDFTestInitializer.java`
5. `iq-apis/src/test/java/systems/symbol/rdf/fixtures/SampleGraphsFixture.java`
6. `iq-apis/src/test/java/systems/symbol/mcp/MCPTestInitializer.java`

### Per-Connector Mocks (High Priority)
7. `iq-connect-aws/src/test/java/systems/symbol/connect/aws/mocks/AwsMockFixture.java`
8. `iq-connect-aws/src/test/java/systems/symbol/connect/aws/AwsConnectorMockIT.java`
9. `iq-connect-azure/src/test/java/systems/symbol/connect/azure/mocks/AzureMockFixture.java`
10. `iq-connect-azure/src/test/java/systems/symbol/connect/azure/AzureConnectorMockIT.java`
11. `iq-connect-gcp/src/test/java/systems/symbol/connect/gcp/mocks/GcpMockFixture.java`
12. `iq-connect-gcp/src/test/java/systems/symbol/connect/gcp/GcpConnectorMockIT.java`
... and 25 more for other connectors

---

## 13. Next Steps

1. **Schedule**: Assign 1-2 developers for 4-5 weeks (Phases 1-5)
2. **Priority**: Start with Phase 1 (shared utilities) in Week 1
3. **Blocking**: AWS+Azure mocks (Phase 2) unblock CI validation
4. **Documentation**: Update README in each connector module with test examples
5. **Validation**: Run `mvn clean install -DskipTests=false` locally before PR
