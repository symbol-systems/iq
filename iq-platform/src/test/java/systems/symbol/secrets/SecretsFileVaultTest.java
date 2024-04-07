package systems.symbol.secrets;

import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class SecretsFileVaultTest {
@Test
void testVault() throws IOException, SecretsException {
File testHome = new File("tmp/secrets");
testHome.mkdirs();
I_SecretsStore vault = new OpenSecretsFileVault(testHome);
I_Secrets iSecrets = vault.setSecrets("default", new SimpleSecrets());
assert null != iSecrets;
iSecrets.setSecret("key", "secret");

boolean denied = false;
try {
assert null != iSecrets.getSecret("default", "key");
} catch (Exception e) {
denied = true;
}
assert denied;
iSecrets.grant("key", "default");
assert null != iSecrets.getSecret("key", "default");
assert iSecrets.getSecret("key", "default").equals("secret");
}
@Test
void testSaveSecrets() throws IOException, SecretsException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
File testHome = new File("tmp/secrets");
testHome.mkdirs();
I_SecretsStore vault = new OpenSecretsFileVault(testHome);
I_Secrets iSecrets = vault.setSecrets("default", new SimpleSecrets());
assert null!=iSecrets;
iSecrets.setSecret("hello", "world");
vault.save();
}
}