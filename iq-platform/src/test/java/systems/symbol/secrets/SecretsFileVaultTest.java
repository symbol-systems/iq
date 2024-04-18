package systems.symbol.secrets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.COMMONS;

import java.io.File;
import java.io.IOException;

class SecretsFileVaultTest {
IRI self = Values.iri(COMMONS.IQ_NS_TEST);
@Test
void testVault() throws IOException, SecretsException {
File testHome = new File("tmp/secrets");
I_SecretsStore vault = new BasicFileVault(testHome);
SimpleSecrets secrets = new SimpleSecrets();
secrets.setSecret("hello", "world");

I_Secrets secrets2 = vault.setSecrets(self, secrets);
assert null != secrets2;
assert null != secrets.getSecret("hello");
assert secrets.getSecret("hello").equals("world");

}

@Test
void testSaveSecrets() throws IOException {
File testHome = new File("tmp/secrets");
I_SecretsStore vault = new BasicFileVault(testHome);
I_Secrets iSecrets = vault.setSecrets(self, new SimpleSecrets());
assert null!=iSecrets;
vault.setSecrets(self, "hello", "world");
}
}