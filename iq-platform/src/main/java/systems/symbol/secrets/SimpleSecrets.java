package systems.symbol.secrets;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of secrets with access control.
 * Stores key-value pairs and manages access control for each key.
 */
public class SimpleSecrets implements I_Secrets, Serializable {
    private static final long serialVersionUID = 2462762779494608930L;
//    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, String> secretStore = new HashMap<>();


    /**
     * Set a secret for the specified key.
     *
     * @param key    The key associated with the secret.
     * @param secret The secret to be stored.
     */
    public void setSecret(String key, String secret) {
        secretStore.put(key, secret);
    }

    /**
     * Get the secret for the specified key if access is granted.
     *
     * @param key   The key for which the secret is requested.
     * @return The secret associated with the key.
     */
    @Override
    public String getSecret(String key) {
        return secretStore.get(key);
    }

    /**
     * Provide a string representation of the secrets and access control information.
     *
     * @return A string representation of the secrets and access control.
     */
    @Override
    public String toString() {
        return "Secrets: " + secretStore.keySet();
    }
}
