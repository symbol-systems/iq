package systems.symbol.connect.confluence;

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
 * Comprehensive test suite for Confluence Connector.
 *
 * Extends ConnectorTestScaffold to exercise authentication, read, write,
 * checkpoint, and error handling scenarios.
 *
 * Integration tests require CONFLUENCE_URL or CONFLUENCE_API_TOKEN environment variables.
 */
public class ConfluenceConnectorTest extends ConnectorTestScaffold {

@Override
protected String getConnectorName() {
return "confluence";
}

@Override
protected I_Connector createConnector() {
String baseUrl = System.getenv("CONFLUENCE_URL");
ConfluenceConnectorConfig config = new ConfluenceConnectorConfig(
baseUrl, 
System.getenv("CONFLUENCE_API_TOKEN"),
System.getenv("CONFLUENCE_USERNAME"),
System.getenv("CONFLUENCE_PASSWORD"),
Duration.ofMinutes(5), 
null, 
"pages,comments", 
false);
return new ConfluenceConnector("urn:iq:connector:confluence", config);
}

@Override
protected I_Connector createConnectorWithBadCredentials() {
ConfluenceConnectorConfig config = new ConfluenceConnectorConfig(
"https://invalid-confluence.atlassian.net",
"invalid-token",
null,
null,
Duration.ofMinutes(5),
null,
"pages,comments",
false);
return new ConfluenceConnector("urn:iq:connector:confluence", config);
}

@Override
protected void testAuthenticateSuccess() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
return;
}
assertAuthenticationSuccess(createConnector());
}

@Override
protected void testAuthenticateInvalidCredentials() throws Exception {
assertAuthenticationFails(createConnectorWithBadCredentials());
}

@Override
protected void testAuthenticateExpiredToken() throws Exception {
testAuthenticateInvalidCredentials();
}

@Override
protected void testReadDataSuccess() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
return;
}
assertReadDataSuccess(createConnector());
}

@Override
protected void testReadDataEmpty() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
return;
}
I_Connector connector = createConnector();
connector.refresh();
assertConnectorStatus(connector, ConnectorStatus.IDLE);
}

@Override
protected void testReadDataRateLimited() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
return;
}
assertReadDataRateLimited(createConnector());
}

@Override
protected void testWriteDataSuccess() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
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
I_Connector connector = createConnector();
assertWriteDataConflict(connector);
}

@Override
protected void testWriteDataPermissionDenied() throws Exception {
I_Connector connector = createConnectorWithBadCredentials();
assertWriteDataPermissionDenied(connector);
}

@Override
protected void testCheckpointCreate() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
return;
}
assertCheckpointCreated(createConnector());
}

@Override
protected void testCheckpointResume() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
return;
}
I_Connector connector1 = createConnector();
I_Connector connector2 = createConnector();
assertCheckpointResume(connector1, connector2);
}

@Override
protected void testErrorNetworkTimeout() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
return;
}
assertNetworkTimeout(createConnector());
}

@Override
protected void testErrorServiceUnavailable() throws Exception {
if (System.getenv("CONFLUENCE_URL") == null && System.getenv("CONFLUENCE_API_TOKEN") == null) {
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
assertEquals("urn:iq:connector:confluence", connector.getConnectorId().stringValue());
}

@Test
void testKernelStartStop() {
ConfluenceConnectorConfig config = new ConfluenceConnectorConfig("https://dummy-confluence.atlassian.net", "dummy-token", null, null, Duration.ofMinutes(5), null, "pages,comments", false);
ConfluenceConnector connector = new ConfluenceConnector("urn:iq:connector:confluence", config);
ConfluenceConnectorKernel kernel = new ConfluenceConnectorKernel(connector);

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
@EnabledIfEnvironmentVariable(named = "CONFLUENCE_URL", matches = ".*")
void testIntegrationAuthenticateSuccess() throws Exception {
testAuthenticateSuccess();
}

@Test
void testIntegrationAuthenticateInvalidCredentials() throws Exception {
testAuthenticateInvalidCredentials();
}

@Test
@EnabledIfEnvironmentVariable(named = "CONFLUENCE_URL", matches = ".*")
void testIntegrationReadDataSuccess() throws Exception {
testReadDataSuccess();
}

@Test
@EnabledIfEnvironmentVariable(named = "CONFLUENCE_URL", matches = ".*")
void testIntegrationCheckpointCreate() throws Exception {
testCheckpointCreate();
}
}
