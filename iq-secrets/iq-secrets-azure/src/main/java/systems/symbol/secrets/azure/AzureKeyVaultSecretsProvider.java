package systems.symbol.secrets.azure;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.I_SecretsStore;
import systems.symbol.secrets.SecretsException;
import systems.symbol.secrets.SecretsStoreFactory;
import systems.symbol.secrets.SimpleSecrets;

import java.util.Map;
import java.util.Optional;

public class AzureKeyVaultSecretsProvider implements I_SecretsStore {
private final SecretClient client;
private final String prefix;

public AzureKeyVaultSecretsProvider() {
String vaultUrl = Optional.ofNullable(System.getenv("AZURE_KEYVAULT_URL"))
.orElseThrow(() -> new IllegalStateException("AZURE_KEYVAULT_URL is required"));
this.prefix = Optional.ofNullable(System.getenv("IQ_SECRETS_PREFIX")).orElse("iq");
this.client = new SecretClientBuilder()
.vaultUrl(vaultUrl)
.credential(new DefaultAzureCredentialBuilder().build())
.buildClient();
}

@Override
public I_Secrets getSecrets(IRI agent) throws SecretsException {
String agentId = SecretsStoreFactory.encodeAgent(agent);
SimpleSecrets secrets = new SimpleSecrets();
try {
client.listPropertiesOfSecrets().forEach(prop -> {
String name = prop.getName();
String marker = prefix + "-" + agentId + "-";
if (name.startsWith(marker)) {
try {
KeyVaultSecret secret = client.getSecret(name);
if (secret != null && secret.getValue() != null) {
String key = name.substring(marker.length());
secrets.setSecret(key, secret.getValue());
}
} catch (Exception ignore) {
// ignore missing or permission errors for individual secrets
}
}
});
return secrets;
} catch (Exception e) {
throw new SecretsException("Failed to read secrets from Azure Key Vault", e);
}
}

@Override
public void setSecrets(IRI agent, String key, String value) {
String agentId = SecretsStoreFactory.encodeAgent(agent);
String name = SecretsStoreFactory.safeSecretName(prefix, agentId, key);
// For azure secret names there is a max length; we use safe-encoded values by default.
client.setSecret(new KeyVaultSecret(name, value));
}

@Override
public void setSecrets(IRI agent, I_Secrets secrets) {
if (secrets == null) {
return;
}
if (!(secrets instanceof SimpleSecrets)) {
throw new IllegalArgumentException("AzureKeyVaultSecretsProvider only supports SimpleSecrets for batch set");
}
for (Map.Entry<String, String> entry : ((SimpleSecrets) secrets).getAllSecrets().entrySet()) {
setSecrets(agent, entry.getKey(), entry.getValue());
}
}
}

