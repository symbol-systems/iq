package systems.symbol.connect.github;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import systems.symbol.connect.core.ConnectorScanner;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;

final class GithubOrganizationScanner implements ConnectorScanner<GithubScanContext> {

private final GHOrganization organization;

GithubOrganizationScanner(GHOrganization organization) {
this.organization = organization;
}

@Override
public void scan(GithubScanContext context) throws IOException {
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