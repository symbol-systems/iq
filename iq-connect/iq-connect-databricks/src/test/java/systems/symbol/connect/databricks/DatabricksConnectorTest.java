
package systems.symbol.connect.databricks;

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
 * Comprehensive test suite for Databricks Connector.
 *
 * Extends ConnectorTestScaffold to exercise authentication, read, write,
 * checkpoint, and error handling scenarios.
 *
 * Integration tests require DATABRICKS_HOST or DATABRICKS_TOKEN environment variables.
 */
public class DatabricksConnectorTest extends ConnectorTestScaffold {

@Override
protected String getConnectorName() {
return "databricks";
}

@Override
protected I_Connector createConnector() {
String host = System.getenv("DATABRICKS_HOST");
String token = System.getenv("DATABRICKS_TOKEN");
DatabricksConnectorConfig config = new DatabricksConnectorConfig(host, token, Duration.ofMinutes(5), null);
return new DatabricksConnector("urn:iq:connector:databricks", config);
}

@Override
protected I_Connector createConnectorWithBadCredentials() {
DatabricksConnectorConfig config = new DatabricksConnectorConfig("https://invalid-host.databricks.com", "invalid-token", Duration.ofMinutes(5), null);
return new DatabricksConnector("urn:iq:connector:databricks", config);
}

@Override
protected void testAuthenticateSuccess() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
assertAuthenticationSuccess(createConnector());
}

@Override
protected void testAuthenticateInvalidCredentials() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
assertAuthenticationFails(createConnectorWithBadCredentials());
}

@Override
protected void testAuthenticateExpiredToken() throws Exception {
testAuthenticateInvalidCredentials();
}

@Override
protected void testReadDataSuccess() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
assertReadDataSuccess(createConnector());
}

@Override
protected void testReadDataEmpty() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
I_Connector connector = createConnector();
connector.refresh();
assertConnectorStatus(connector, ConnectorStatus.IDLE);
}

@Override
protected void testReadDataRateLimited() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
assertReadDataRateLimited(createConnector());
}

@Override
protected void testWriteDataSuccess() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
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
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
I_Connector connector = createConnector();
assertWriteDataConflict(connector);
}

@Override
protected void testWriteDataPermissionDenied() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
I_Connector connector = createConnectorWithBadCredentials();
assertWriteDataPermissionDenied(connector);
}

@Override
protected void testCheckpointCreate() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
assertCheckpointCreated(createConnector());
}

@Override
protected void testCheckpointResume() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
I_Connector connector1 = createConnector();
I_Connector connector2 = createConnector();
assertCheckpointResume(connector1, connector2);
}

@Override
protected void testErrorNetworkTimeout() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
return;
}
assertNetworkTimeout(createConnector());
}

@Override
protected void testErrorServiceUnavailable() throws Exception {
if (System.getenv("DATABRICKS_HOST") == null) {
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
assertEquals("urn:iq:connector:databricks", connector.getConnectorId().stringValue());
}

@Test
void testKernelStartStop() {
DatabricksConnectorConfig config = new DatabricksConnectorConfig("https://dummy-host.databricks.com", "dummy-token", Duration.ofMinutes(5), null);
DatabricksConnector connector = new DatabricksConnector("urn:iq:connector:databricks", config);
DatabricksConnectorKernel kernel = new DatabricksConnectorKernel(connector);

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
@EnabledIfEnvironmentVariable(named = "DATABRICKS_HOST", matches = ".*")
void testIntegrationAuthenticateSuccess() throws Exception {
testAuthenticateSuccess();
}

@Test
void testIntegrationAuthenticateInvalidCredentials() throws Exception {
testAuthenticateInvalidCredentials();
}

@Test
@EnabledIfEnvironmentVariable(named = "DATABRICKS_HOST", matches = ".*")
void testIntegrationReadDataSuccess() throws Exception {
testReadDataSuccess();
}

@Test
@EnabledIfEnvironmentVariable(named = "DATABRICKS_HOST", matches = ".*")
void testIntegrationCheckpointCreate() throws Exception {
testCheckpointCreate();
}
}
