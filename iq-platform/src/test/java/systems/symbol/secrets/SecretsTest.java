package systems.symbol.secrets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.testng.annotations.Test;
import systems.symbol.COMMONS;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class SecretsTest {
IRI self = Values.iri(COMMONS.IQ_NS_TEST);

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