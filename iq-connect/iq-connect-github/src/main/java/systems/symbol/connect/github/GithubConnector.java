package systems.symbol.connect.github;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.Modeller;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;

import systems.symbol.connect.core.ConnectorMode;

/**
 * Example GitHub connector implementation.
 */
public final class GithubConnector extends AbstractConnector {

private final GithubConfig config;

public GithubConnector(String connectorId, GithubConfig config, Model state) {
this(connectorId,
config,
state,
Values.iri(connectorId + "/graph/current"),
Values.iri(Modeller.getGithubOntology()),
Values.iri("urn:github:"));
}

public GithubConnector(String connectorId, GithubConfig config, Model state, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
super(connectorId, state, graphIri, ontologyBaseIri, entityBaseIri);
this.config = config;
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
protected void doRefresh() throws Exception {
IRI connectorId = getConnectorId();
GitHub github = GitHub.connectUsingOAuth(config.getAccessToken());
GithubModeller modeller = new GithubModeller(getModel(), graphIri(), ontologyBaseIri(), entityBaseIri());

if (config.getOrganization().isPresent()) {
GHOrganization organization = github.getOrganization(config.getOrganization().get());
IRI orgIri = modeller.organization(connectorId, organization.getLogin(), organization.getName());

for (GHRepository repo : organization.listRepositories()) {
addRepository(repo, orgIri, modeller);
}

for (GHUser member : organization.listMembers()) {
addUser(member, orgIri, modeller);
}

for (GHTeam team : organization.listTeams()) {
addTeam(team, orgIri, organization.getLogin(), modeller);
}
} else {
GHMyself me = github.getMyself();
IRI userIri = modeller.rootUser(connectorId, me.getLogin(), me.getName());

for (GHRepository repo : me.listRepositories()) {
addRepository(repo, userIri, modeller);
}

for (GHOrganization org : me.getAllOrganizations()) {
IRI orgIri = modeller.organization(org.getLogin(), org.getName());
modeller.linkResource(userIri, orgIri);
}
}
}

private void addRepository(GHRepository repo, IRI ownerIri, GithubModeller modeller) {
try {
IRI repoIri = modeller.repository(
connectorId,
ownerIri,
repo.getName(),
repo.getFullName(),
repo.isPrivate(),
repo.getForksCount(),
repo.getStargazersCount(),
repo.getOpenIssueCount(),
repo.getDefaultBranch());

repo.getBranches().forEach((name, branch) -> {
modeller.branch(repoIri, repo.getFullName(), name, branch.isProtected());
});

for (org.kohsuke.github.GHHook h : repo.getHooks()) {
String hookUrl = null;
if (h.getConfig() != null) {
hookUrl = h.getConfig().get("url");
}
modeller.webHook(repoIri, repo.getFullName(), h.getId(), hookUrl, h.isActive());
}

// Collaborators and access control
for (GHUser collaborator : repo.listCollaborators()) {
modeller.collaborator(repoIri, collaborator.getLogin());
}

// Topics/tags for repository governance visibility
for (String topic : repo.listTopics()) {
modeller.topic(repoIri, repo.getFullName(), topic);
}
} catch (IOException e) {
// best-effort ignore PMS and continue the sync; preserve status field metadata in outer catch
}
}

private void addUser(GHUser user, IRI orgIri, GithubModeller modeller) {
try {
modeller.orgUser(orgIri, user.getLogin(), user.getName());
} catch (IOException e) {
// best-effort metadata extraction
}
}

private void addTeam(GHTeam team, IRI orgIri, String orgLogin, GithubModeller modeller) {
IRI teamIri = modeller.team(orgIri, orgLogin + ":" + team.getSlug(), team.getName(), String.valueOf(team.getPrivacy()));

try {
for (GHUser member : team.getMembers()) {
modeller.teamMember(teamIri, member.getLogin());
}
} catch (IOException e) {
// best-effort skip member listing when unavailable
}
}
}