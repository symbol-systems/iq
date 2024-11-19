package systems.symbol.secrets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.platform.I_LoadSave;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

/**
 * Implementation of a Secrets Vault that stores secrets in plain text files.
 */
public class PlainPasswordVault extends MemoryVault implements I_LoadSave {
private File vaultHome;

protected PlainPasswordVault() {
}

/**
 * Constructor that takes the vault home directory as a parameter.
 *
 * @param vaultHome The directory where secrets will be stored.
 * @throws FileNotFoundException If the vault home is not a directory or cannot
 *   be created.
 */
public PlainPasswordVault(File vaultHome) throws IOException {
vaultHome.mkdirs();
log.info("vault.home: {} -> {}", vaultHome.getAbsolutePath(), vaultHome.exists());
if ((!vaultHome.exists() || vaultHome.isFile())) {
throw new FileNotFoundException("Vault home must be a folder, not a file: " + vaultHome.getAbsolutePath());
}
this.vaultHome = vaultHome;
load();
}

@Override
public I_Secrets getSecrets(IRI agent) {
I_Secrets iSecrets = store.get(agent);
if (iSecrets != null)
return iSecrets;
return new EnvsAsSecrets();
}

/**
 * Save all secrets to plain text files.
 *
 * @throws IOException If an I/O error occurs during the save operation.
 */
public void save() throws IOException {
checkVaultHome();
for (IRI agent : this.store.keySet()) {
I_Secrets iSecrets = store.get(agent);
save(agent, iSecrets);
}
}

/**
 * Load secrets from plain text files.
 *
 * @throws IOException If an I/O error occurs during the load operation.
 */
public void load() throws IOException {
checkVaultHome();
File[] files = vaultHome.listFiles();
if (files != null) {
for (File file : files) {
load(Values.iri(file.toURI().toASCIIString()));
}
}
}

/**
 * Save secrets to a plain text file.
 *
 * @param agentThe name of the owner for whom secrets are being saved.
 * @param iSecrets The secrets to be saved.
 * @throws IOException If an I/O error occurs during the save operation.
 */
protected void save(IRI agent, I_Secrets iSecrets) throws IOException {
try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(
Files.newOutputStream(toFile(agent).toPath()))) {
objectOutputStream.writeObject(iSecrets);
} catch (URISyntaxException e) {
throw new RuntimeException(e);
}
}

/**
 * Load secrets from a plain text file.
 *
 * @param agent The agent of the owner for whom secrets are being loaded.
 * @throws IOException If an I/O error occurs during the load operation.
 */
protected void load(IRI agent) throws IOException {
try (ObjectInputStream objectInputStream = new ObjectInputStream(
Files.newInputStream(toFile(agent).toPath()))) {
I_Secrets secrets = (I_Secrets) objectInputStream.readObject();
this.store.put(agent, secrets);
} catch (ClassNotFoundException | URISyntaxException e) {
throw new RuntimeException(e);
}
}

/**
 * Convert a vault name into a file.
 *
 * @param agent The ID of the agent for whom secrets are being stored.
 * @return The File object representing the file where secrets will be stored.
 */
private File toFile(IRI agent) throws URISyntaxException {
return new File(new URI(agent.stringValue()));
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
