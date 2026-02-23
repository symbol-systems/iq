package systems.symbol.secrets;

import org.apache.commons.vfs2.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.platform.I_LoadSave;

import java.io.*;
import java.net.URISyntaxException;

/**
 * Implementation of a Secrets Vault that stores secrets in plain text files in
 * a VFS Filesystem.
 */
public class VFSPasswordVault extends MemoryVault implements I_LoadSave {
    protected FileObject vaultHome;
    protected FileSystemManager fsm;

    public VFSPasswordVault() throws FileSystemException, IOException {
        this(new systems.symbol.vfs.MyVFS(), "vfs.vault");
    }

    /**
     * Constructor that takes the VFS file system, vault home directory as a
     * parameter.
     *
     * @param vaultHomeURI The URI of the directory where secrets will be stored.
     * @throws FileSystemException If the vault home cannot be accessed or created.
     */
    public VFSPasswordVault(FileSystemManager fsm, String vaultHomeURI) throws FileSystemException, IOException {
        this(fsm, fsm.resolveFile(vaultHomeURI));
    }

    public VFSPasswordVault(FileSystemManager fsm, FileObject vaultHome) throws FileSystemException, IOException {
        this.fsm = fsm;
        this.vaultHome = vaultHome;

        if (!vaultHome.exists()) {
            vaultHome.createFolder();
        }
        log.info("vault.vfs.home: {} -> {}", vaultHome.getName().getURI(), vaultHome.exists());
        if (!vaultHome.isFolder()) {
            throw new FileSystemException("Vault home must be a folder: " + vaultHome.getName().getURI());
        }
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
     * Save all secrets to plain text files in VFS.
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
     * Load secrets from plain text files in VFS.
     *
     * @throws IOException If an I/O error occurs during the load operation.
     */
    public void load() throws IOException {
        checkVaultHome();
        for (FileObject file : vaultHome.getChildren()) {
            load(Values.iri(file.getName().getURI()));
        }
    }

    /**
     * Save secrets to a plain text file in VFS.
     *
     * @param agent    The name of the owner for whom secrets are being saved.
     * @param iSecrets The secrets to be saved.
     * @throws IOException If an I/O error occurs during the save operation.
     */
    protected void save(IRI agent, I_Secrets iSecrets) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(
                toFileObject(agent).getContent().getOutputStream())) {
            objectOutputStream.writeObject(iSecrets);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load secrets from a plain text file in VFS.
     *
     * @param agent The agent of the owner for whom secrets are being loaded.
     * @throws IOException If an I/O error occurs during the load operation.
     */
    protected void load(IRI agent) throws IOException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(
                toFileObject(agent).getContent().getInputStream())) {
            I_Secrets secrets = (I_Secrets) objectInputStream.readObject();
            this.store.put(agent, secrets);
        } catch (ClassNotFoundException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert an agent ID into a VFS file object.
     *
     * @param agent The ID of the agent for whom secrets are being stored.
     * @return The FileObject representing the file where secrets will be stored.
     */
    private FileObject toFileObject(IRI agent) throws FileSystemException, URISyntaxException {
        FileSystemManager fsManager = VFS.getManager();
        return fsManager.resolveFile(vaultHome, agent.stringValue());
    }

    /**
     * Check if the vault home directory exists.
     *
     * @throws FileSystemException If the vault home is not found or not accessible.
     */
    private void checkVaultHome() throws FileSystemException {
        if (this.vaultHome == null || !this.vaultHome.exists()) {
            throw new FileSystemException("Vault home not found: " +
                    (this.vaultHome == null ? "null" : vaultHome.getName().getURI()));
        }
    }
}
