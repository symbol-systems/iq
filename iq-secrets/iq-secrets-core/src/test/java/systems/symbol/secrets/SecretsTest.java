package systems.symbol.secrets;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import junit.framework.TestCase;

public class SecretsTest extends TestCase {

    public void testSecretsHelper() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ClassNotFoundException, SecretsException {
        // Create a sample I_Secrets object for testing
        SimpleSecrets sampleSecrets = new SimpleSecrets();
        sampleSecrets.setSecret("hello", "world");
        // Test encryption and decryption
        String secret = sampleSecrets.getSecret("hello");
        assertEquals("world", secret);
    }


}