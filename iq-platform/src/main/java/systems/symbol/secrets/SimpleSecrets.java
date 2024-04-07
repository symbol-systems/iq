package systems.symbol.secrets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of secrets with access control.
 * Stores key-value pairs and manages access control for each key.
 */
public class SimpleSecrets implements I_Secrets, Serializable {
    private static final long serialVersionUID = 2462762779494608930L;
    private final Logger log = LoggerFactory.getLogger(getClass());

    // Store secret key-value pairs
    private final Map<String, String> secretStore = new HashMap<>();

    // Access control map to manage secret access
    private final Map<String, Boolean> accessControl = new HashMap<>();

    /**
     * Set a secret for the specified key.
     *
     * @param key    The key associated with the secret.
     * @param secret The secret to be stored.
     */
    @Override
    public void setSecret(String key, String secret) {
        secretStore.put(key, secret);
    }

    /**
     * Get the secret for the specified key if access is granted.
     *
     * @param key   The key for which the secret is requested.
     * @param agent The agent attempting to access the secret.
     * @return The secret associated with the key.
     * @throws SecretsException If access is denied.
     */
    @Override
    public String getSecret(String key, String agent) throws SecretsException {
        guard(key, agent);
        return secretStore.get(key);
    }

    /**
     * Check if the agent has access to the specified key.
     *
     * @param key   The key for which access is checked.
     * @param agent The agent attempting to access the key.
     * @throws SecretsException If access is denied.
     */
    protected void guard(String key, String agent) throws SecretsException {
        if (!granted(key, agent)) {
            throw new SecretsException("agent.access.denied");
        }
    }

    /**
     * Grant access to the specified key for the agent.
     *
     * @param key   The key for which access is granted.
     * @param agent The agent to whom access is granted.
     */
    @Override
    public void grant(String key, String agent) {
        // Grant access by adding the composite key to the access control map
        accessControl.put(key + "&" + agent, true);
    }

    /**
     * Revoke access to the specified key for the agent.
     *
     * @param key   The key for which access is revoked.
     * @param agent The agent from whom access is revoked.
     */
    @Override
    public void revoke(String key, String agent) {
        // Revoke access by removing the composite key from the access control map
        String compositeKey = key + "&" + agent;
        accessControl.remove(compositeKey);
    }

    /**
     * Check if access is granted to the specified key for the agent.
     *
     * @param key   The key for which access is checked.
     * @param agent The agent for whom access is checked.
     * @return True if access is granted, false otherwise.
     */
    @Override
    public boolean granted(String key, String agent) {
        // Check if access is granted based on the composite key
        String compositeKey = key + "&" + agent;
        return accessControl.containsKey(compositeKey);
    }

    /**
     * Provide a string representation of the secrets and access control information.
     *
     * @return A string representation of the secrets and access control.
     */
    @Override
    public String toString() {
        return "Secrets: " + secretStore.keySet() + " & Access Control: " + accessControl.keySet();
    }
}
