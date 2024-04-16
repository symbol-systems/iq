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
    public I_Secrets setSecrets(IRI agent, String key, String value) {
        return setSecrets(agent, new SimpleSecrets());
    }

    @Override
    public I_Secrets setSecrets(IRI agent, I_Secrets secrets) {
        store.put(agent, secrets);
        return secrets;
    }
}
