package systems.symbol.secrets.gcp;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1.Secret;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretName;
import com.google.cloud.secretmanager.v1.ProjectName;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.cloud.secretmanager.v1.Replication;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient.ListSecretsPagedResponse;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.I_SecretsStore;
import systems.symbol.secrets.SecretsException;
import systems.symbol.secrets.SecretsStoreFactory;
import systems.symbol.secrets.SimpleSecrets;

import java.util.Map;
import java.util.Optional;

public class GcpSecretManagerProvider implements I_SecretsStore {

private final SecretManagerServiceClient client;
private final String projectId;
private final String prefix;

public GcpSecretManagerProvider() {
this.projectId = Optional.ofNullable(System.getenv("GOOGLE_CLOUD_PROJECT"))
.orElse(Optional.ofNullable(System.getenv("GCLOUD_PROJECT")).orElseThrow(
() -> new IllegalStateException("GOOGLE_CLOUD_PROJECT or GCLOUD_PROJECT is required")));
this.prefix = Optional.ofNullable(System.getenv("IQ_SECRETS_PREFIX")).orElse("iq");
try {
this.client = SecretManagerServiceClient.create();
} catch (Exception e) {
throw new RuntimeException("Failed to create GCP Secret Manager client", e);
}
}

@Override
public I_Secrets getSecrets(IRI agent) throws SecretsException {
String agentId = SecretsStoreFactory.encodeAgent(agent);
String marker = prefix + "-" + agentId + "-";
SimpleSecrets secrets = new SimpleSecrets();

try {
ListSecretsPagedResponse response = client.listSecrets(ProjectName.of(projectId));
for (Secret secret : response.iterateAll()) {
String name = secret.getName();
// identifer last path segment
String shortName = name.substring(name.lastIndexOf("/") + 1);
if (!shortName.startsWith(marker)) {
continue;
}
String key = shortName.substring(marker.length());
SecretVersionName versionName = SecretVersionName.of(projectId, shortName, "latest");
AccessSecretVersionResponse access = client.accessSecretVersion(versionName);
String value = access.getPayload().getData().toStringUtf8();
if (value != null) {
secrets.setSecret(key, value);
}
}
return secrets;
} catch (Exception e) {
throw new SecretsException("Failed to read secrets from GCP Secret Manager", e);
}
}

@Override
public void setSecrets(IRI agent, String key, String value) {
String agentId = SecretsStoreFactory.encodeAgent(agent);
String shortName = SecretsStoreFactory.safeSecretName(prefix, agentId, key);
String parent = ProjectName.of(projectId).toString();

try {
// create secret if not exists
SecretName secretName = SecretName.of(projectId, shortName);
boolean exists;
try {
client.getSecret(secretName);
exists = true;
} catch (Exception e) {
exists = false;
}
if (!exists) {
client.createSecret(CreateSecretRequest.newBuilder()
.setParent(ProjectName.of(projectId).toString())
.setSecretId(shortName)
.setSecret(Secret.newBuilder()
.setReplication(Replication.newBuilder()
.setAutomatic(Replication.Automatic.getDefaultInstance())
.build())
.build())
.build());
}
client.addSecretVersion(AddSecretVersionRequest.newBuilder()
.setParent(secretName.toString())
.setPayload(SecretPayload.newBuilder().setData(com.google.protobuf.ByteString.copyFromUtf8(value)).build())
.build());
} catch (Exception e) {
throw new RuntimeException("Failed to set secret in GCP Secret Manager", e);
}
}

@Override
public void setSecrets(IRI agent, I_Secrets secrets) {
if (secrets == null) {
return;
}
if (!(secrets instanceof SimpleSecrets)) {
throw new IllegalArgumentException("GcpSecretManagerProvider only supports SimpleSecrets for batch set");
}
for (Map.Entry<String, String> entry : ((SimpleSecrets) secrets).getAllSecrets().entrySet()) {
setSecrets(agent, entry.getKey(), entry.getValue());
}
}

private String sanitizeSecretId(String s) {
return s.replaceAll("[^A-Za-z0-9-_]", "-").replaceAll("^-+|-+$", "");
}
}

