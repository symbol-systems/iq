package systems.symbol.secrets.hashicorp;

import systems.symbol.secrets.I_SecretsStore;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import org.eclipse.rdf4j.model.IRI;

public class HashicorpVaultSecretsProvider implements I_SecretsStore {

@Override
public I_Secrets getSecrets(IRI agent) throws SecretsException {
throw new UnsupportedOperationException("Hashicorp Vault provider implementation required");
}

@Override
public void setSecrets(IRI agent, String key, String value) {
throw new UnsupportedOperationException("Hashicorp Vault provider implementation required");
}

@Override
public void setSecrets(IRI agent, I_Secrets secrets) {
throw new UnsupportedOperationException("Hashicorp Vault provider implementation required");
}
}
