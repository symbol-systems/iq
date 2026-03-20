package systems.symbol.secrets.aws;

import org.eclipse.rdf4j.model.IRI;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.SecretListEntry;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.I_SecretsStore;
import systems.symbol.secrets.SecretsException;
import systems.symbol.secrets.SecretsStoreFactory;
import systems.symbol.secrets.SimpleSecrets;

import java.util.Map;
import java.util.Optional;

public class AwsSecretsManagerProvider implements I_SecretsStore {
    private final SecretsManagerClient client;
    private final String prefix;

    public AwsSecretsManagerProvider() {
        String regionName = Optional.ofNullable(System.getenv("AWS_REGION"))
                .orElse(Optional.ofNullable(System.getenv("AWS_DEFAULT_REGION")).orElse("us-east-1"));
        this.prefix = Optional.ofNullable(System.getenv("IQ_SECRETS_PREFIX")).orElse("iq");
        this.client = SecretsManagerClient.builder()
                .region(Region.of(regionName))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Override
    public I_Secrets getSecrets(IRI agent) throws SecretsException {
        String agentId = SecretsStoreFactory.encodeAgent(agent);
        String marker = prefix + "-" + agentId + "-";

        SimpleSecrets secrets = new SimpleSecrets();
        try {
            String nextToken = null;
            do {
                ListSecretsRequest request = ListSecretsRequest.builder()
                        .filters(b -> b.key("name").values(marker).build())
                        .nextToken(nextToken)
                        .build();

                ListSecretsResponse response = client.listSecrets(request);
                for (SecretListEntry entry : response.secretList()) {
                    String name = entry.name();
                    if (name.startsWith(marker)) {
                        try {
                            String value = client.getSecretValue(GetSecretValueRequest.builder().secretId(name).build()).secretString();
                            if (value != null) {
                                secrets.setSecret(name.substring(marker.length()), value);
                            }
                        } catch (ResourceNotFoundException ignore) {
                        }
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);
            return secrets;
        } catch (Exception e) {
            throw new SecretsException("Failed to read secrets from AWS Secrets Manager", e);
        }
    }

    @Override
    public void setSecrets(IRI agent, String key, String value) {
        String agentId = SecretsStoreFactory.encodeAgent(agent);
        String name = SecretsStoreFactory.safeSecretName(prefix, agentId, key);

        try {
            // attempt to update existing secret value
            try {
                client.getSecretValue(GetSecretValueRequest.builder().secretId(name).build());
                client.putSecretValue(PutSecretValueRequest.builder()
                        .secretId(name)
                        .secretString(value)
                        .build());
            } catch (ResourceNotFoundException e) {
                client.createSecret(CreateSecretRequest.builder()
                        .name(name)
                        .secretString(value)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set secret in AWS Secrets Manager", e);
        }
    }

    @Override
    public void setSecrets(IRI agent, I_Secrets secrets) {
        if (secrets == null) {
            return;
        }
        if (!(secrets instanceof SimpleSecrets)) {
            throw new IllegalArgumentException("AwsSecretsManagerProvider only supports SimpleSecrets for batch set");
        }
        for (Map.Entry<String, String> entry : ((SimpleSecrets) secrets).getAllSecrets().entrySet()) {
            setSecrets(agent, entry.getKey(), entry.getValue());
        }
    }
}

