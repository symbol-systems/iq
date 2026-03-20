package systems.symbol.connect.github;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.Modeller;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;

import systems.symbol.connect.core.Checkpoints;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorProvenance;
import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.ConnectorSyncMetadata;
import systems.symbol.connect.core.I_Checkpoint;
import systems.symbol.connect.core.I_Connector;
import systems.symbol.connect.core.I_ConnectorDescriptor;

/**
 * Example GitHub connector implementation.
 */
public final class GithubConnector implements I_Connector, I_ConnectorDescriptor {

private final IRI connectorId;
private final Model state;
private final GithubConfig config;
private final IRI graphIri;
private final IRI ontologyBaseIri;
private final IRI entityBaseIri;

private volatile ConnectorStatus status = ConnectorStatus.IDLE;
private volatile Optional<I_Checkpoint> checkpoint = Optional.empty();

public GithubConnector(String connectorId, GithubConfig config, Model state) {
this(connectorId,
config,
state,
Values.iri(connectorId + "/graph/current"),
Values.iri(Modeller.getGithubOntology()),
Values.iri("urn:github:"));
}

public GithubConnector(String connectorId, GithubConfig config, Model state, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
this.connectorId = Values.iri(connectorId);
this.config = config;
this.state = state;
this.graphIri = graphIri;
this.ontologyBaseIri = ontologyBaseIri;
this.entityBaseIri = entityBaseIri;
}

@Override
public IRI getSelf() {
return connectorId;
}

@Override
public IRI getConnectorId() {
return connectorId;
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
public ConnectorStatus getStatus() {
return status;
}

@Override
public Model getModel() {
return state;
}

@Override
public Optional<I_Checkpoint> getCheckpoint() {
return checkpoint;
}

@Override
public void start() {
status = ConnectorStatus.SYNCING;
}

@Override
public void stop() {
status = ConnectorStatus.IDLE;
}

@Override
public void refresh() {
status = ConnectorStatus.SYNCING;
state.remove(null, null, null, graphIri);
ConnectorSyncMetadata.markSyncing(state, connectorId, graphIri);
IRI activity = ConnectorProvenance.markSyncStarted(state, connectorId, graphIri);

try {
GitHub github = GitHub.connectUsingOAuth(config.getAccessToken());
GithubModeller modeller = new GithubModeller(state, graphIri, ontologyBaseIri, entityBaseIri);

if (config.getOrganization().isPresent()) {
GHOrganization organization = github.getOrganization(config.getOrganization().get());
IRI orgIri = modeller.organization(connectorId, organization.getLogin(), organization.getName());

for (GHRepository repo : organization.listRepositories().toList()) {
addRepository(repo, orgIri, modeller);
}

for (GHUser member : organization.listMembers().toList()) {
addUser(member, orgIri, modeller);
}

for (GHTeam team : organization.listTeams()) {
addTeam(team, orgIri, organization.getLogin(), modeller);
}
} else {
GHMyself me = github.getMyself();
IRI userIri = modeller.rootUser(connectorId, me.getLogin(), me.getName());

for (GHRepository repo : me.listRepositories().toList()) {
addRepository(repo, userIri, modeller);
}

for (GHOrganization org : me.getAllOrganizations()) {
IRI orgIri = modeller.organization(org.getLogin(), org.getName());
modeller.linkResource(userIri, orgIri);
}
}

checkpoint = Optional.of(Checkpoints.of(state));
status = ConnectorStatus.IDLE;
ConnectorSyncMetadata.markSynced(state, connectorId, graphIri);
ConnectorProvenance.markSyncCompleted(state, activity, graphIri);
} catch (Exception e) {
status = ConnectorStatus.ERROR;
ConnectorSyncMetadata.markError(state, connectorId, graphIri);
ConnectorProvenance.markSyncFailed(state, activity, e, graphIri);
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
for (GHUser collaborator : repo.listCollaborators().toList()) {
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

@Override
public String getName() {
return "GitHub Connector";
}

@Override
public String getDescription() {
return "Syncs GitHub organization/repository metadata into IQ.";
}

@Override
public Model getDescriptorModel() {
Model m = new LinkedHashModel();
m.add(connectorId, Modeller.rdfType(), Modeller.connect("Connector"));
m.add(connectorId, Modeller.connect("hasName"), Values.***REMOVED***(getName()));
m.add(connectorId, Modeller.connect("hasDescription"), Values.***REMOVED***(getDescription()));
return m;
}
}