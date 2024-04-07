package systems.symbol.rdf4j.store;

import systems.symbol.platform.Workspace;
import org.eclipse.rdf4j.repository.Repository;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public class RepositoriesTest {
    File repoFolder = new File("tmp/iq.test");

    @Test
    public void testCreateDefaultRepository() throws IOException {
        repoFolder.mkdirs();
        Workspace factory = new Workspace(repoFolder);
        Repository repository = factory.getCurrentRepository();
        System.out.println("repository.default.created: "+repository);
        assert repository!=null;
    }

    @Test
    public void testConfig() {
    }

    @Test
    public void testTestConfig() {
    }

    @Test
    public void testTestConfig1() {
    }

    @Test
    public void testCreateTemplateStream() {
    }
}