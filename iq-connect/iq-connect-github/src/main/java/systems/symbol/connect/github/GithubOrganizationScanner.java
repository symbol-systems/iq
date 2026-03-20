package systems.symbol.connect.github;

import org.eclipse.rdf4j.model.IRI;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;

final class GithubOrganizationScanner {

    private GithubOrganizationScanner() {
    }

    static void scan(GHOrganization organization, GithubScanContext context) {
        IRI orgIri = context.modeller().organization(context.connectorId(), organization.getLogin(), organization.getName());

        for (GHRepository repo : organization.listRepositories()) {
            GithubRepositoryScanner.scan(repo, orgIri, context);
        }

        for (GHUser member : organization.listMembers()) {
            GithubUserScanner.scanOrganizationUser(member, orgIri, context);
        }

        for (GHTeam team : organization.listTeams()) {
            GithubTeamScanner.scan(team, orgIri, organization.getLogin(), context);
        }
    }
}