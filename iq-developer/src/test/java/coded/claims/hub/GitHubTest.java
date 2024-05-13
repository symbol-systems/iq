package systems.symbol.hub;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class GitHubTest {
    String token = "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";

    @Test
    public void testWithLogin() throws IOException {
    }

    // @Test
    public void testWithOAuthToken() throws IOException {
        GitHub gitHub = new GitHub();
        GitHub gitHub1 = gitHub.withToken(token);
        assert gitHub1 != null;
        assert gitHub1.equals(gitHub);
        GHMyself myself = gitHub.getMyself();
        assert myself != null;
        assert myself.getLogin() != null;
        assert myself.getLogin().indexOf("symbol.systems")>=0;
    }

    // @Test
    public void testGetRepository() throws IOException {
        GitHub gitHub = new GitHub();
        gitHub.withToken(token);
        GHRepository repository = gitHub.getRepository("iq-test");
        assert repository != null;
        assert repository.getDefaultBranch() != null;
        System.out.println("codehub.get.repo: "+repository.getDefaultBranch()+" @ "+repository.getFullName());
    }

    // @Test
    public void testCreateRepository() throws IOException {
        GitHub gitHub = new GitHub();
        gitHub.withToken(token);
        String repo = "iq-test-"+System.currentTimeMillis();
        GHRepository repository = gitHub.getOrCreateRepository(repo);
        System.out.println("codehub.create.repo: "+repository.getDefaultBranch()+" @ "+repository.getFullName());

        assert repository != null;
        GHRepository check_repository = gitHub.getRepository(repo);
        assert check_repository != null;
        assert check_repository.getFullName().equals( repository.getFullName());
        repository.delete();
    }

    // @Test
    public void testPushContent() throws IOException {
        GitHub gitHub = new GitHub();
        gitHub.withToken(token);
        File folder = new File("src/test/resources/codehub/");
        gitHub.push("iq-test", folder, new Date().toString());
        System.out.println("codehub.create.push: "+folder.exists()+" -> "+folder.getAbsolutePath());
    }
}