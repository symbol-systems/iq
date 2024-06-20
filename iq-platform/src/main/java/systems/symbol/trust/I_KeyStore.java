package systems.symbol.trust;

import java.io.File;
import java.security.KeyPair;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public interface I_KeyStore extends I_Keys {


    /**
     * Checks if the key pair is provisioned in the specified folder.
     *
     * @return true if both private and public key files exist in the folder, false otherwise
     */
    boolean isProvisioned();

    /**
     * Saves the given KeyPair to the specified folder.
     *
     * @param keyPair the KeyPair to save
     * @throws Exception if an error occurs while saving the keys
     */
    void save(KeyPair keyPair) throws Exception;

    /**
     * Loads a KeyPair from the specified folder.
     *
     * @return the loaded KeyPair
     * @throws Exception if an error occurs while loading the keys
     */
    KeyPair load() throws Exception;

    /**
     * Converts a key to PKCS#8 format.
     *
     * @param key the key to convert
     * @return the key in PKCS#8 format as a String
     * @throws Exception if an error occurs during the conversion
     */
    static String toPKCS8(Key key) throws Exception {
        return SimpleKeyStore.toPKCS8(key);
    }

    /**
     * Converts a PKCS#8 formatted key to a Key object.
     *
     * @param key the key in PKCS#8 format as a String
     * @return the Key object
     * @throws Exception if an error occurs during the conversion
     */
    static Key fromPKCS8(String key) throws Exception {
        return SimpleKeyStore.fromPKCS8(key);
    }
}
