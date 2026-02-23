/**
 * Interface for managing secrets in IQ.
 *
 * The secrets store provides functionality for securely managing sensitive information
 * associated with agents in the IQ system. This includes retrieving, setting, and updating
 * secrets for specific agents.
 *
 * This interface defines the core methods required to interact with the secrets store.
 *
 * Methods:
 * - {@code getSecrets(IRI agent)}: Retrieves the secrets associated with the specified agent.
 * - {@code setSecrets(IRI agent, String key, String value)}: Sets a secret key-value pair for the specified agent.
 * - {@code setSecrets(IRI agent, I_Secrets secrets)}: Sets the secrets for the specified agent using an I_Secrets instance.
 */

package systems.symbol.secrets;

import org.eclipse.rdf4j.model.IRI;

public interface I_SecretsStore {

    /**
     * Retrieves the secrets associated with the specified agent.
     *
     * @param agent the IRI representing the agent whose secrets are to be retrieved.
     * @return the secrets associated with the specified agent.
     * @throws SecretsException if an error occurs while retrieving the secrets.
     */
    I_Secrets getSecrets(IRI agent) throws SecretsException;

    /**
     * Sets a secret key-value pair for the specified agent.
     *
     * @param agent the IRI representing the agent for which the secret is to be set.
     * @param key   the key of the secret.
     * @param value the value of the secret.
     */
    void setSecrets(IRI agent, String key, String value);

    /**
     * Sets the secrets for the specified agent.
     *
     * @param agent   the IRI representing the agent for which the secrets are to be set.
     * @param secrets an instance of I_Secrets containing the secrets to be set.
     * @return an instance of I_Secrets representing the updated secrets for the agent.
     */
    void setSecrets(IRI agent, I_Secrets secrets);

}
