package systems.symbol.connect.github;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;

final class GithubRepositoryScanner {

    private GithubRepositoryScanner() {
    }

    static void scan(GHRepository repo, IRI ownerIri, GithubScanContext context) {
        try {
            GithubModeller modeller = context.modeller();
            IRI repoIri = modeller.repository(
                context.connectorId(),
                ownerIri,
                repo.getName(),
                repo.getFullName(),
                repo.isPrivate(),
                repo.getForksCount(),
                repo.getStargazersCount(),
                repo.getOpenIssueCount(),
                repo.getDefaultBranch());

            repo.getBranches().forEach((name, branch) -> modeller.branch(repoIri, repo.getFullName(), name, branch.isProtected()));

            for (org.kohsuke.github.GHHook hook : repo.getHooks()) {
                String hookUrl = null;
                if (hook.getConfig() != null) {
                    hookUrl = hook.getConfig().get("url");
                }
                modeller.webHook(repoIri, repo.getFullName(), hook.getId(), hookUrl, hook.isActive());
            }

            for (GHUser collaborator : repo.listCollaborators()) {
                modeller.collaborator(repoIri, collaborator.getLogin());
            }

            for (String topic : repo.listTopics()) {
                modeller.topic(repoIri, repo.getFullName(), topic);
            }
        } catch (IOException e) {
            // best-effort repository scan
        }
    }
}