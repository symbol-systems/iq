package systems.symbol.trust;

import systems.symbol.secrets.SecretsException;

import java.security.KeyPair;

public interface I_Keys {

    /**
     * Retrieves/generates a new KeyPair.
     *
     * @return the KeyPair
     * @throws SecretsException
     */
    KeyPair keys() throws SecretsException;

}
