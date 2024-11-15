package systems.symbol.secrets;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.realm.RealmManager;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class RealmManagerTest {

    private FileSystemManager fileSystemManager;
    private File tempDirectory;
    private RealmManager realmManager;

    @BeforeEach
    public void setUp() throws Exception {
        fileSystemManager = VFS.getManager();

        tempDirectory = new File(System.getProperty("java.io.tmpdir"), "testVaultHome");
        if (!tempDirectory.exists()) {
            tempDirectory.mkdir();
        }

        realmManager = new RealmManager();
    }

    @Test
    public void testVaultHomeFolderCreation() throws Exception {
        assertDoesNotThrow(() -> realmManager.getVaultHome());

        FileObject vaultHome = fileSystemManager.resolveFile(tempDirectory.getPath());
        assertTrue(vaultHome.exists(), "Vault home folder should be created");
    }
}
