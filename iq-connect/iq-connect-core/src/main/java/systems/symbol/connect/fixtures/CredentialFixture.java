package systems.symbol.connect.fixtures;

/**
 * Mock credentials for connector integration testing.
 * 
 * Provides standardized mock credential values for all integrated services.
 * These are test-only credentials that should never be used in production.
 * 
 * AWS Examples:
 *   Access Key: AKIAIOSFODNN7EXAMPLE
 *   Secret Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
 *   (These are AWS's official test credentials from documentation)
 * 
 * @author Symbol Systems
 */
public class CredentialFixture {

// AWS Credentials (official AWS test examples)
public static String mockAWSAccessKeyId() {
return "AKIAIOSFODNN7EXAMPLE";
}

public static String mockAWSSecretAccessKey() {
return "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
}

public static String mockAWSRegion() {
return "us-east-1";
}

public static String mockAWSBucket() {
return "test-bucket-12345";
}

// Azure Credentials
public static String mockAzureSubscriptionId() {
return "12345678-1234-1234-1234-123456789000";
}

public static String mockAzureTenantId() {
return "87654321-4321-4321-4321-000987654321";
}

public static String mockAzureClientId() {
return "11111111-1111-1111-1111-111111111111";
}

public static String mockAzureClientSecret() {
return "azure_test_secret_xxxxxxxxxxxxxxxxxxxx";
}

public static String mockAzureResourceGroup() {
return "test-resource-group";
}

// GCP Credentials
public static String mockGCPProjectId() {
return "test-project-123456";
}

public static String mockGCPServiceAccountEmail() {
return "test-sa@test-project-123456.iam.gserviceaccount.com";
}

public static String mockGCPPrivateKeyId() {
return "1234567890abcdef1234567890abcdef12345678";
}

public static String mockGCPPrivateKey() {
return "-----BEGIN PRIVATE KEY-----\n" +
   "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7W8x...\n" +
   "-----END PRIVATE KEY-----";
}

// Snowflake Credentials
public static String mockSnowflakeAccount() {
return "xy12345.us-east-1";
}

public static String mockSnowflakeUser() {
return "test_user";
}

public static String mockSnowflakePassword() {
return "TestPassword123!@#";
}

public static String mockSnowflakeDatabase() {
return "TEST_DB";
}

public static String mockSnowflakeWarehouse() {
return "TEST_WH";
}

// Slack Credentials
public static String mockSlackBotToken() {
return "***REMOVED***";
}

public static String mockSlackAppToken() {
return "xapp-1-xxxxxxxxxxxxxxxxxxxx";
}

public static String mockSlackTeamId() {
return "T0123456789";
}

// GitHub Credentials
public static String mockGitHubToken() {
return "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
}

public static String mockGitHubOwner() {
return "test-owner";
}

public static String mockGitHubRepository() {
return "test-repo";
}

// Salesforce Credentials
public static String mockSalesforceClientId() {
return "3MVG9d8..KmX";
}

public static String mockSalesforceClientSecret() {
return "1234567890ABCDEF";
}

public static String mockSalesforceUsername() {
return "test@example.com";
}

public static String mockSalesforcePassword() {
return "TestPassword123!@#";
}

// Generic OAuth
public static String mockOAuthClientId() {
return "test-client-id-1234567890";
}

public static String mockOAuthClientSecret() {
return "test-client-secret-abcdefghijklmnop";
}

public static String mockOAuthRedirectUrl() {
return "http://localhost:8080/callback";
}

// Database Credentials
public static String mockDatabaseUser() {
return "testuser";
}

public static String mockDatabasePassword() {
return "TestPassword123!@#";
}

public static String mockDatabaseUrl() {
return "jdbc:postgresql://localhost:5432/testdb";
}

// API Keys
public static String mockApiKey() {
return "sk-test-1234567890abcdefghijklmnopqrs";
}

public static String mockApiSecret() {
return "sk_test_abcdefghijklmnopqrstuvwxyz123456";
}

// Bearer Token
public static String mockBearerToken() {
return "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
}

}
