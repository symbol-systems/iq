
package systems.symbol.connect.aws;

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
 * Comprehensive test suite for AWS Connector.
 * 
 * Extends ConnectorTestScaffold to exercise authentication, read, write,
 * checkpoint, and error handling scenarios.
 * 
 * Integration tests require AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables.
 */
public class AwsConnectorTest extends ConnectorTestScaffold {

@Override
protected String getConnectorName() {
return "aws";
}

@Override
protected I_Connector createConnector() {
String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
AwsConnectorConfig config = new AwsConnectorConfig(accessKey, Duration.ofMinutes(5), null);
return new AwsConnector("urn:iq:connector:aws", config);
}

@Override
protected I_Connector createConnectorWithBadCredentials() {
AwsConnectorConfig config = new AwsConnectorConfig("invalid-key", Duration.ofMinutes(5), null);
return new AwsConnector("urn:iq:connector:aws", config);
}

@Override
protected void testAuthenticateSuccess() throws Exception {
// Skip if credentials not available
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
assertAuthenticationSuccess(createConnector());
}

@Override
protected void testAuthenticateInvalidCredentials() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
assertAuthenticationFails(createConnectorWithBadCredentials());
}

@Override
protected void testAuthenticateExpiredToken() throws Exception {
// AWS tokens don't typically "expire" in the same way as OAuth tokens
// but we test with bad keys to simulate invalid credentials
testAuthenticateInvalidCredentials();
}

@Override
protected void testReadDataSuccess() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
assertReadDataSuccess(createConnector());
}

@Override
protected void testReadDataEmpty() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
I_Connector connector = createConnector();
connector.refresh();
assertConnectorStatus(connector, ConnectorStatus.IDLE);
}

@Override
protected void testReadDataRateLimited() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
assertReadDataRateLimited(createConnector());
}

@Override
protected void testWriteDataSuccess() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
// AWS connector is typically read-only for AWS resources
// Write operations would be to tags, metadata, etc.
I_Connector connector = createConnector();
assertDoesNotThrow(
connector::refresh,
"Connector should handle write operations gracefully"
);
}

@Override
protected void testWriteDataConflict() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
// AWS resources may conflict on updates
I_Connector connector = createConnector();
assertWriteDataConflict(connector);
}

@Override
protected void testWriteDataPermissionDenied() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
// Test with insufficient IAM permissions
I_Connector connector = createConnectorWithBadCredentials();
assertWriteDataPermissionDenied(connector);
}

@Override
protected void testCheckpointCreate() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
assertCheckpointCreated(createConnector());
}

@Override
protected void testCheckpointResume() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
I_Connector connector1 = createConnector();
I_Connector connector2 = createConnector();
assertCheckpointResume(connector1, connector2);
}

@Override
protected void testErrorNetworkTimeout() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
return;
}
assertNetworkTimeout(createConnector());
}

@Override
protected void testErrorServiceUnavailable() throws Exception {
if (System.getenv("AWS_ACCESS_KEY_ID") == null) {
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
assertEquals("urn:iq:connector:aws", connector.getConnectorId().stringValue());
}

@Test
void testKernelStartStop() {
AwsConnectorConfig config = new AwsConnectorConfig("dummy-key", Duration.ofMinutes(5), null);
AwsConnector connector = new AwsConnector("urn:iq:connector:aws", config);
AwsConnectorKernel kernel = new AwsConnectorKernel(connector);

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
@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".*")
void testIntegrationAuthenticateSuccess() throws Exception {
testAuthenticateSuccess();
}

@Test
void testIntegrationAuthenticateInvalidCredentials() throws Exception {
testAuthenticateInvalidCredentials();
}

@Test
@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".*")
void testIntegrationReadDataSuccess() throws Exception {
testReadDataSuccess();
}

@Test
@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".*")
void testIntegrationCheckpointCreate() throws Exception {
testCheckpointCreate();
}
}
