/**
 * Interface for representing secrets associated with an agent in the IQ operating environment.
 *
 * This interface defines the method for retrieving a secret based on a provided key.
 *
 */

package systems.symbol.secrets;

import java.util.Collections;
import java.util.Map;

public interface I_Secrets {

/**
 * Retrieves the value of the secret associated with the specified key.
 *
 * @param key the key of the secret.
 * @return the value of the secret associated with the specified key, or {@code null} if the key is not found.
 */
String getSecret(String key);

/**
 * Returns all secrets in this object as an immutable map. Optional operation.
 * Default implementation returns empty map.
 */
default Map<String, String> getAllSecrets() {
return Collections.emptyMap();
}
}

