package systems.symbol.secrets;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a Secrets Vault that stores secrets in plain text files.
 */
public class OpenSecretsFileVault implements I_SecretsStore {
    private final Map<String, I_Secrets> ownerSecrets = new HashMap<>();
    private File vaultHome;

    // Default constructor
    protected OpenSecretsFileVault() {
    }

    /**
     * Constructor that takes the vault home directory as a parameter.
     *
     * @param vaultHome The directory where secrets will be stored.
     * @throws FileNotFoundException If the vault home is not a directory or cannot be created.
     */
    public OpenSecretsFileVault(File vaultHome) throws FileNotFoundException {
        if (vaultHome.exists() && vaultHome.isFile()) {
            throw new FileNotFoundException("Vault home must be a folder, not a file: " + vaultHome.getAbsolutePath());
        }
        this.vaultHome = vaultHome;
        this.vaultHome.mkdirs(); // Create the directory if it doesn't exist
    }

    @Override
    public I_Secrets getSecrets(String owner) {
        return ownerSecrets.get(owner);
    }

    @Override
    public I_Secrets setSecrets(String owner, I_Secrets secrets) {
        ownerSecrets.put(owner, secrets);
        return secrets;
    }

    /**
     * Save all secrets to plain text files.
     *
     * @throws IOException If an I/O error occurs during the save operation.
     */
    public void save() throws IOException {
        checkVaultHome();

        for (String name : this.ownerSecrets.keySet()) {
            I_Secrets iSecrets = ownerSecrets.get(name);
            if (iSecrets != null) {
                save(name, iSecrets);
            }
        }
    }

    /**
     * Load secrets from plain text files.
     *
     * @throws IOException              If an I/O error occurs during the load operation.
     * @throws ClassNotFoundException    If the class of a serialized object cannot be found during deserialization.
     */
    public void load() throws IOException, ClassNotFoundException {
        checkVaultHome();
        File[] files = vaultHome.listFiles();
        if (files != null) {
            for (File file : files) {
                load(file.getName());
            }
        }
    }

    /**
     * Save secrets to a plain text file.
     *
     * @param name     The name of the owner for whom secrets are being saved.
     * @param iSecrets The secrets to be saved.
     * @throws IOException If an I/O error occurs during the save operation.
     */
    protected void save(String name, I_Secrets iSecrets) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                Files.newOutputStream(toFile(name).toPath()))) {
            objectOutputStream.writeObject(iSecrets);
        }
    }

    /**
     * Load secrets from a plain text file.
     *
     * @param name The name of the owner for whom secrets are being loaded.
     * @throws IOException              If an I/O error occurs during the load operation.
     * @throws ClassNotFoundException    If the class of a serialized object cannot be found during deserialization.
     */
    protected void load(String name) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(
                Files.newInputStream(toFile(name).toPath()))) {
            I_Secrets secrets = (I_Secrets) objectInputStream.readObject();
            ownerSecrets.put(name, secrets);
        }
    }

    /**
     * Convert a vault name into a file.
     *
     * @param name The name of the owner for whom secrets are being stored.
     * @return The File object representing the file where secrets will be stored.
     */
    private File toFile(String name) {
        return new File(this.vaultHome, name);
    }

    /**
     * Check if the vault home directory exists.
     *
     * @throws FileNotFoundException If the vault home is not found.
     */
    private void checkVaultHome() throws FileNotFoundException {
        if (this.vaultHome == null || !this.vaultHome.exists()) {
            throw new FileNotFoundException("Vault home not found: " +
                    (this.vaultHome == null ? "null" : this.vaultHome.getAbsolutePath()));
        }
    }
}
