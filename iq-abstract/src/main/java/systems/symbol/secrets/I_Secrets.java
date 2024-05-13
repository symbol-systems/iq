/**
 * Interface for representing secrets associated with an agent in the IQ operating environment.
 *
 * This interface defines the method for retrieving a secret based on a provided key.
 *
 */

package systems.symbol.secrets;

public interface I_Secrets {

   /**
* Retrieves the value of the secret associated with the specified key.
*
* @param key the key of the secret.
* @return the value of the secret associated with the specified key, or {@code null} if the key is not found.
*/
   String getSecret(String key);
}
