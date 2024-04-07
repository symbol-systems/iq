package systems.symbol.secrets;

import org.testng.annotations.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecretsTest {

@Test
public void testSecretsHelper() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, IOException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ClassNotFoundException, SecretsException {
// Create a sample I_Secrets object for testing
I_Secrets sampleSecrets = new EnvsAsSecrets();
sampleSecrets.setSecret("hello", "world");
sampleSecrets.grant("hello", "jo");
// Test encryption and decryption
String secret = sampleSecrets.getSecret("hello", "jo");
assert "world".equals(secret);
}


}