package systems.symbol.vfs;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MyVFSTest {

    private MyVFS myVFS;

    @BeforeEach
    public void setUp() throws FileSystemException {
        myVFS = new MyVFS();
    }

    @Test
    public void testInit() throws FileSystemException {
        assertNotNull(myVFS.getFilesCache());
        assertTrue(myVFS.hasProvider("href"));
        assertTrue(myVFS.hasProvider("http"));
        assertTrue(myVFS.hasProvider("https"));
        assertTrue(myVFS.hasProvider("s3"));
        assertTrue(myVFS.hasProvider("file"));
    }

    @Test
    public void testResolveFile() throws FileSystemException {
        FileObject fileObject = myVFS.resolveFile("src/test");
        System.out.printf("vfs.fo.found: %s == %s\n", fileObject.getPublicURIString(), fileObject.getType());
        assertTrue(fileObject.exists());
    }
}
