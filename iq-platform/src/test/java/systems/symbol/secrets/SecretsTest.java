package systems.symbol.secrets;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

public class SecretsTest {
    @Test
    public void testSecretsHelper() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ClassNotFoundException, SecretsException {
        // Create a sample I_Secrets object for testing
        SimpleSecrets sampleSecrets = new SimpleSecrets();
        sampleSecrets.setSecret("hello", "world");
        // Test encryption and decryption
        String secret = sampleSecrets.getSecret("hello");
        assert "world".equals(secret);
    }


}