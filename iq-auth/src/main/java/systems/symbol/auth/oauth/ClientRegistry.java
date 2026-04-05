package systems.symbol.auth.oauth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client Registry for OAuth clients.
 * Stores and validates OAuth client credentials.
 */
public class ClientRegistry {

public static class OAuthClient {
public final String clientId;
public final String clientSecret;
public final Set<String> redirectUris;
public final Set<String> grantTypes;
public final Set<String> scopes;
public final String name;
public final boolean isPublic; // Public clients don't have secrets

public OAuthClient(String clientId, String clientSecret, Set<String> redirectUris,
  Set<String> grantTypes, Set<String> scopes, String name, boolean isPublic) {
this.clientId = Objects.requireNonNull(clientId);
this.clientSecret = clientSecret;
this.redirectUris = Collections.unmodifiableSet(redirectUris != null ? redirectUris : new HashSet<>());
this.grantTypes = Collections.unmodifiableSet(grantTypes != null ? grantTypes : new HashSet<>());
this.scopes = Collections.unmodifiableSet(scopes != null ? scopes : new HashSet<>());
this.name = name;
this.isPublic = isPublic;
}
}

private final Map<String, OAuthClient> clients = new ConcurrentHashMap<>();

/**
 * Register a new OAuth client.
 */
public void register(OAuthClient client) {
Objects.requireNonNull(client, "client is required");
clients.put(client.clientId, client);
}

/**
 * Get a client by ID.
 */
public OAuthClient getClient(String clientId) {
return clients.get(clientId);
}

/**
 * Validate client credentials (for confidential clients).
 */
public boolean validateClientSecret(String clientId, String clientSecret) {
OAuthClient client = getClient(clientId);
if (client == null) return false;
if (client.isPublic) return false; // Public clients don't have secrets
return constantTimeEquals(client.clientSecret, clientSecret);
}

/**
 * Check if client is authorized for a grant type.
 */
public boolean isGrantTypeAllowed(String clientId, String grantType) {
OAuthClient client = getClient(clientId);
if (client == null) return false;
return client.grantTypes.contains(grantType);
}

/**
 * Check if redirect URI is registered for client.
 */
public boolean isRedirectUriAllowed(String clientId, String redirectUri) {
OAuthClient client = getClient(clientId);
if (client == null) return false;
return client.redirectUris.contains(redirectUri);
}

/**
 * Filter requested scopes to allowed scopes for client.
 */
public Set<String> filterScopes(String clientId, Set<String> requestedScopes) {
OAuthClient client = getClient(clientId);
if (client == null || requestedScopes == null) return Collections.emptySet();

Set<String> allowed = new HashSet<>();
for (String scope : requestedScopes) {
if (client.scopes.contains(scope)) {
allowed.add(scope);
}
}
return allowed;
}

/**
 * Constant-time string comparison to prevent timing attacks.
 */
private boolean constantTimeEquals(String a, String b) {
if (a == null || b == null) return false;
int result = 0;
byte[] aBytes = a.getBytes();
byte[] bBytes = b.getBytes();
int minLength = Math.min(aBytes.length, bBytes.length);

for (int i = 0; i < minLength; i++) {
result |= aBytes[i] ^ bBytes[i];
}
result |= aBytes.length ^ bBytes.length;
return result == 0;
}
}
