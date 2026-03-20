package systems.symbol.secrets;

import org.eclipse.rdf4j.model.IRI;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import systems.symbol.vfs.MyVFS;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class SecretsStoreFactory {

    public static I_SecretsStore createDefault(File home) throws Exception {
        String backend = Optional.ofNullable(System.getenv("IQ_SECRETS_BACKEND")).orElse("local");
        return create(backend, home);
    }

    public static I_SecretsStore create(String backend, File home) throws Exception {
        if (backend == null || backend.isBlank() || "local".equalsIgnoreCase(backend)) {
            return createLocal(home);
        }
        String normalized = backend.trim().toLowerCase();
        switch (normalized) {
            case "hashicorp":
                return instantiate("systems.symbol.secrets.hashicorp.HashicorpVaultSecretsProvider");
            case "azure":
                return instantiate("systems.symbol.secrets.azure.AzureKeyVaultSecretsProvider");
            case "aws":
                return instantiate("systems.symbol.secrets.aws.AwsSecretsManagerProvider");
            case "gcp":
                return instantiate("systems.symbol.secrets.gcp.GcpSecretManagerProvider");
            default:
                throw new IllegalArgumentException("Unknown IQ_SECRETS_BACKEND: " + backend);
        }
    }

    public static I_SecretsStore createLocal(File home) throws Exception {
        FileSystemManager fsm = new MyVFS();
        FileObject vaultPath = fsm.resolveFile(home, "vault/secrets");
        return new VFSPasswordVault(fsm, vaultPath);
    }

    @SuppressWarnings("unchecked")
    private static I_SecretsStore instantiate(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (I_SecretsStore) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Secret backend class not found: " + className, e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize secret backend: " + className, e);
        }
    }

    public static String encodeAgent(IRI agent) {
        try {
            return URLEncoder.encode(agent.stringValue(), StandardCharsets.UTF_8.toString()).replaceAll("%", "-");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String safeSecretName(String prefix, String agentId, String key) {
        try {
            String safeKey = URLEncoder.encode(key, StandardCharsets.UTF_8.toString()).replaceAll("%", "-");
            return String.format("%s-%s-%s", prefix, agentId, safeKey);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String safeSecretName(String agentId, String key) {
        return safeSecretName("iq", agentId, key);
    }
}

