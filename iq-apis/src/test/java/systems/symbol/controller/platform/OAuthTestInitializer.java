package systems.symbol.controller.platform;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.auth.oauth.ClientRegistry;
import systems.symbol.auth.oauth.OAuthAuthorizationServer;

import java.util.*;

/**
 * Test startup handler that registers OAuth test clients.
 * Only active when running in test environment (picked up from test classpath).
 */
@ApplicationScoped
public class OAuthTestInitializer {

private static final Logger log = LoggerFactory.getLogger(OAuthTestInitializer.class);

@Inject
OAuthAuthorizationServer authServer;

void onStart(@Observes StartupEvent startup) {
// Only register test clients if not already present
ClientRegistry registry = authServer.getClientRegistry();

if (registry.getClient("cli") == null) {
// Register CLI test client with secret (for client_credentials flow)
ClientRegistry.OAuthClient cliTestClient = new ClientRegistry.OAuthClient(
"cli",
"secret",
new HashSet<>(Collections.singletonList("http://localhost:8080/callback")),
new HashSet<>(Collections.singletonList("client_credentials")),
new HashSet<>(Arrays.asList(
"chat.read", "chat.write", "agent.trigger",
"sparql.select", "sparql.update",
"connector.execute",
"control.read", "control.write"
)),
"CLI Test Client",
false // confidential client
);
registry.register(cliTestClient);
log.info("oauth.test.client.registered: cli");
}

if (registry.getClient("test-client") == null) {
// Register generic test client
ClientRegistry.OAuthClient testClient = new ClientRegistry.OAuthClient(
"test-client",
"secret",
new HashSet<>(Collections.singletonList("http://localhost:8080/callback")),
new HashSet<>(Collections.singletonList("client_credentials")),
new HashSet<>(Arrays.asList(
"chat.read", "chat.write",
"sparql.select", "sparql.update"
)),
"Test Client",
false // confidential client
);
registry.register(testClient);
log.info("oauth.test.client.registered: test-client");
}
}
}
