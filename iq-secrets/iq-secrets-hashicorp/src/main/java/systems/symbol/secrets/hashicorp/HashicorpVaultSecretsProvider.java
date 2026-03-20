package systems.symbol.secrets.hashicorp;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.I_SecretsStore;
import systems.symbol.secrets.SecretsException;
import systems.symbol.secrets.SecretsStoreFactory;
import systems.symbol.secrets.SimpleSecrets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HashicorpVaultSecretsProvider implements I_SecretsStore {
    private final Vault vault;
    private final String basePath;

    public HashicorpVaultSecretsProvider() {
        try {
            String address = Optional.ofNullable(System.getenv("VAULT_ADDR")).orElse("http://127.0.0.1:8200");
            String token = Optional.ofNullable(System.getenv("VAULT_TOKEN")).orElseThrow(() -> new IllegalStateException("VAULT_TOKEN is required for HashiCorp provider"));
            String secretEngine = Optional.ofNullable(System.getenv("VAULT_SECRET_ENGINE")).orElse("secret");
            this.basePath = secretEngine.endsWith("/") ? secretEngine : secretEngine + "/";

            VaultConfig config = new VaultConfig().address(address).token(token).build();
            this.vault = new Vault(config);
        } catch (VaultException e) {
            throw new IllegalStateException("Unable to initialize HashiCorp Vault client", e);
        }
    }

    @Override
    public I_Secrets getSecrets(IRI agent) throws SecretsException {
        try {
            String agentId = SecretsStoreFactory.encodeAgent(agent);
            String path = this.basePath + "iq/" + agentId;
            LogicalResponse response = vault.logical().list(path + "/");
            SimpleSecrets secrets = new SimpleSecrets();
            if (response != null && response.getListData() != null) {
                for (String key : response.getListData()) {
                    String secretName = path + "/" + key;
                    LogicalResponse dataResponse = vault.logical().read(secretName);
                    if (dataResponse != null && dataResponse.getData() != null) {
                        String value = dataResponse.getData().get("value");
                        if (value != null) {
                            secrets.setSecret(key, value);
                        }
                    }
                }
            }
            return secrets;
        } catch (VaultException e) {
            throw new SecretsException("Failed to read secrets from HashiCorp Vault: " + e.getMessage());
        }
    }

    @Override
    public void setSecrets(IRI agent, String key, String value) {
        try {
            String agentId = SecretsStoreFactory.encodeAgent(agent);
            String secretName = this.basePath + "iq/" + agentId + "/" + key;
            Map<String, Object> payload = new HashMap<>();
            payload.put("value", value);
            vault.logical().write(secretName, payload);
        } catch (VaultException e) {
            throw new RuntimeException("Failed to write secret to HashiCorp Vault", e);
        }
    }

    @Override
    public void setSecrets(IRI agent, I_Secrets secrets) {
        if (secrets == null) {
            return;
        }
        if (!(secrets instanceof SimpleSecrets)) {
            throw new IllegalArgumentException("HashicorpVaultSecretsProvider only supports SimpleSecrets for batch set");
        }
        SimpleSecrets simple = (SimpleSecrets) secrets;
        simple.getAllSecrets().forEach((k, v) -> setSecrets(agent, k, v));
    }
}

