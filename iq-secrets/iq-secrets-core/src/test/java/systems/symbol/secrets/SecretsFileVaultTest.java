package systems.symbol.secrets;

import junit.framework.TestCase;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.platform.IQ_NS;

import java.io.File;
import java.io.IOException;

public class SecretsFileVaultTest extends TestCase {
IRI self = Values.iri(IQ_NS.TEST);

public void testVault() throws IOException, SecretsException {
File testHome = new File("tmp/secrets");
I_SecretsStore vault = new PlainPasswordVault(testHome);
SimpleSecrets secrets = new SimpleSecrets();
secrets.setSecret("hello", "world");

vault.setSecrets(self, secrets);
assert null != secrets.getSecret("hello");
assert secrets.getSecret("hello").equals("world");

}

public void testSaveSecrets() throws IOException, SecretsException {
File testHome = new File("tmp/secrets");
I_SecretsStore vault = new PlainPasswordVault(testHome);
vault.setSecrets(self, new SimpleSecrets());
vault.setSecrets(self, "hello", "world");
//assert  "world".equals(vault.getSecrets(self).getSecret("hello"));
}
}