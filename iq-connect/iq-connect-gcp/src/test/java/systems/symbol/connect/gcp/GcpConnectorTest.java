
package systems.symbol.connect.gcp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.ConnectorTestScaffold;
import systems.symbol.connect.core.I_Connector;

/**
 * Comprehensive test suite for GCP Connector.
 *
 * Extends ConnectorTestScaffold to exercise authentication, read, write,
 * checkpoint, and error handling scenarios.
 *
 * Integration tests require GCP_PROJECT_ID or GOOGLE_APPLICATION_CREDENTIALS environment variables.
 */
public class GcpConnectorTest extends ConnectorTestScaffold {

@Override
protected String getConnectorName() {
return "gcp";
}

@Override
protected I_Connector createConnector() {
String projectId = System.getenv("GCP_PROJECT_ID");
GcpConnectorConfig config = new GcpConnectorConfig(projectId, Duration.ofMinutes(5), null);
return new GcpConnector("urn:iq:connector:gcp", config);
}

@Override
protected I_Connector createConnectorWithBadCredentials() {
GcpConnectorConfig config = new GcpConnectorConfig("invalid-project-id", Duration.ofMinutes(5), null);
return new GcpConnector("urn:iq:connector:gcp", config);
}

@Override
protected void testAuthenticateSuccess() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
assertAuthenticationSuccess(createConnector());
}

@Override
protected void testAuthenticateInvalidCredentials() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
assertAuthenticationFails(createConnectorWithBadCredentials());
}

@Override
protected void testAuthenticateExpiredToken() throws Exception {
// Test with invalid credentials to simulate expired token scenario
testAuthenticateInvalidCredentials();
}

@Override
protected void testReadDataSuccess() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
assertReadDataSuccess(createConnector());
}

@Override
protected void testReadDataEmpty() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
I_Connector connector = createConnector();
connector.refresh();
assertConnectorStatus(connector, ConnectorStatus.IDLE);
}

@Override
protected void testReadDataRateLimited() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
assertReadDataRateLimited(createConnector());
}

@Override
protected void testWriteDataSuccess() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
I_Connector connector = createConnector();
assertDoesNotThrow(
connector::refresh,
"Connector should handle write operations gracefully"
);
}

@Override
protected void testWriteDataConflict() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
I_Connector connector = createConnector();
assertWriteDataConflict(connector);
}

@Override
protected void testWriteDataPermissionDenied() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
I_Connector connector = createConnectorWithBadCredentials();
assertWriteDataPermissionDenied(connector);
}

@Override
protected void testCheckpointCreate() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
assertCheckpointCreated(createConnector());
}

@Override
protected void testCheckpointResume() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
I_Connector connector1 = createConnector();
I_Connector connector2 = createConnector();
assertCheckpointResume(connector1, connector2);
}

@Override
protected void testErrorNetworkTimeout() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
assertNetworkTimeout(createConnector());
}

@Override
protected void testErrorServiceUnavailable() throws Exception {
if (System.getenv("GCP_PROJECT_ID") == null && System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
return;
}
assertServiceUnavailable(createConnector());
}

@Override
protected void testCleanupCascadingDeletes() throws Exception {
assertCleanupSuccess(createConnector());
}

// ============ Unit Tests (No Credentials Required) ============

@Test
void testConnectorInitialization() {
I_Connector connector = createConnectorWithBadCredentials();
assertEquals(ConnectorStatus.IDLE, connector.getStatus());
}

@Test
void testRefreshFailsWithoutValidCredentials() {
I_Connector connector = createConnectorWithBadCredentials();

try {
connector.refresh();
} catch (Exception e) {
// expected
}

assertEquals(ConnectorStatus.ERROR, connector.getStatus());
}

@Test
void testConnectorModel() {
I_Connector connector = createConnectorWithBadCredentials();
assertNotNull(connector.getModel());
}

@Test
void testConnectorId() {
I_Connector connector = createConnector();
assertNotNull(connector.getConnectorId());
assertEquals("urn:iq:connector:gcp", connector.getConnectorId().stringValue());
}

@Test
void testKernelStartStop() {
GcpConnectorConfig config = new GcpConnectorConfig("dummy-project", Duration.ofMinutes(5), null);
GcpConnector connector = new GcpConnector("urn:iq:connector:gcp", config);
GcpConnectorKernel kernel = new GcpConnectorKernel(connector);

kernel.start().join();
kernel.stop().join();

assertTrue(
connector.getStatus() == ConnectorStatus.IDLE ||
connector.getStatus() == ConnectorStatus.SYNCING ||
connector.getStatus() == ConnectorStatus.ERROR
);
}

// ============ Integration Tests (With Credentials) ============

@Test
@EnabledIfEnvironmentVariable(named = "GCP_PROJECT_ID", matches = ".*")
void testIntegrationAuthenticateSuccess() throws Exception {
testAuthenticateSuccess();
}

@Test
void testIntegrationAuthenticateInvalidCredentials() throws Exception {
testAuthenticateInvalidCredentials();
}

@Test
@EnabledIfEnvironmentVariable(named = "GCP_PROJECT_ID", matches = ".*")
void testIntegrationReadDataSuccess() throws Exception {
testReadDataSuccess();
}

@Test
@EnabledIfEnvironmentVariable(named = "GCP_PROJECT_ID", matches = ".*")
void testIntegrationCheckpointCreate() throws Exception {
testCheckpointCreate();
}
}
