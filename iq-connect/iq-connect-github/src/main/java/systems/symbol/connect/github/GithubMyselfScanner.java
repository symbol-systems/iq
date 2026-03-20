package systems.symbol.connect.github;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;

final class GithubMyselfScanner {

    private GithubMyselfScanner() {
    }

    static void scan(GHMyself me, GithubScanContext context) throws IOException {
        IRI userIri = context.modeller().rootUser(context.connectorId(), me.getLogin(), me.getName());

        for (GHRepository repo : me.listRepositories()) {
            GithubRepositoryScanner.scan(repo, userIri, context);
        }

        for (GHOrganization organization : me.getAllOrganizations()) {
            IRI orgIri = context.modeller().organization(organization.getLogin(), organization.getName());
            context.modeller().linkResource(userIri, orgIri);
        }
    }
}