package systems.symbol.secrets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Simple implementation of secrets with access control.
 * Stores key-value pairs and manages access control for each key.
 */
public class SafeSecrets implements I_Secrets, Serializable {
    I_Secrets secrets;

    public SafeSecrets(ByteArrayInputStream in, String password) throws InvalidKeyException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException, ClassNotFoundException, IOException {
        this.secrets = SecretsHelper.decrypt(in, password);
    }

    public byte[] save(I_Secrets secrets, String password, ByteArrayOutputStream out) throws InvalidKeyException,
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException, IOException {
        byte[] encrypted = SecretsHelper.encrypt(secrets, password);
        out.write(encrypted, 0, encrypted.length);
        return encrypted;
    }

    /**
     * Get the secret for the specified key if access is granted.
     *
     * @param key The key for which the secret is requested.
     * @return The secret associated with the key.
     */
    @Override
    public String getSecret(String key) {
        return secrets.getSecret(key);
    }

    /**
     * Provide a string representation of the secrets and access control
     * information.
     *
     * @return A string representation of the secrets and access control.
     */
    @Override
    public String toString() {
        return "[secret]";
    }
}
