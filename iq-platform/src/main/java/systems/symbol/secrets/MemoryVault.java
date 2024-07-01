package systems.symbol.secrets;

import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MemoryVault implements I_SecretsStore {
    Map<IRI, I_Secrets> store = new HashMap<>();

    public MemoryVault() {}

    @Override
    public I_Secrets getSecrets(IRI agent) {
        return System::getenv;
    }

    @Override
    public void setSecrets(IRI agent, String key, String value) {
        setSecrets(agent, new SimpleSecrets());
    }

    @Override
    public void setSecrets(IRI agent, I_Secrets secrets) {
        store.put(agent, secrets);
    }

    @Override
    public String toString() {
        return "vaults:" + store.keySet();
    }
}
