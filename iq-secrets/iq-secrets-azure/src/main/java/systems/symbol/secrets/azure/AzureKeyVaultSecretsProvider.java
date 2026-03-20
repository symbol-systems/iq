package systems.symbol.secrets.azure;

import systems.symbol.secrets.I_SecretsStore;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import org.eclipse.rdf4j.model.IRI;

public class AzureKeyVaultSecretsProvider implements I_SecretsStore {

@Override
public I_Secrets getSecrets(IRI agent) throws SecretsException {
throw new UnsupportedOperationException("Azure Key Vault provider implementation required");
}

@Override
public void setSecrets(IRI agent, String key, String value) {
throw new UnsupportedOperationException("Azure Key Vault provider implementation required");
}

@Override
public void setSecrets(IRI agent, I_Secrets secrets) {
throw new UnsupportedOperationException("Azure Key Vault provider implementation required");
}
}
