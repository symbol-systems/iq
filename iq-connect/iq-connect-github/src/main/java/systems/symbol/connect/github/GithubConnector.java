package systems.symbol.connect.github;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.Modeller;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;

import systems.symbol.connect.core.Checkpoints;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.ConnectorStatus;
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

    private volatile ConnectorStatus status = ConnectorStatus.IDLE;
    private volatile Optional<I_Checkpoint> checkpoint = Optional.empty();

    public GithubConnector(String connectorId, GithubConfig config, Model state) {
        this.connectorId = Values.iri(connectorId);
        this.config = config;
        this.state = state;
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

        try {
            GitHub github = GitHub.connectUsingOAuth(config.getAccessToken());

            state.clear();
            state.add(connectorId, Values.iri(ConnectorModels.HAS_SYNC_STATUS), Values.literal("SYNCING"));
            state.add(connectorId, Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.literal(Instant.now().toString()));

            if (config.getOrganization().isPresent()) {
                GHOrganization organization = github.getOrganization(config.getOrganization().get());
                IRI orgIri = Modeller.toDomainURN("github:org", organization.getLogin());
                state.add(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), orgIri);
                state.add(orgIri, Modeller.rdfType(), Modeller.github("Organization"));
                state.add(orgIri, Modeller.github("name"), Values.literal(organization.getName()));
                state.add(orgIri, Modeller.github("login"), Values.literal(organization.getLogin()));

                for (GHRepository repo : organization.listRepositories().toList()) {
                    addRepository(repo, orgIri);
                }

                for (GHUser member : organization.listMembers().toList()) {
                    addUser(member, orgIri);
                }

                for (GHTeam team : organization.listTeams()) {
                    addTeam(team, orgIri);
                }
            } else {
                GHMyself me = github.getMyself();
                IRI userIri = Modeller.toDomainURN("github:user", me.getLogin());
                state.add(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), userIri);
                state.add(userIri, Modeller.rdfType(), Modeller.github("User"));
                state.add(userIri, Modeller.github("name"), Values.literal(me.getName()));
                state.add(userIri, Modeller.github("login"), Values.literal(me.getLogin()));

                for (GHRepository repo : me.listRepositories().toList()) {
                    addRepository(repo, userIri);
                }

                for (GHOrganization org : me.getAllOrganizations()) {
                    IRI orgIri = Modeller.toDomainURN("github:org", org.getLogin());
                    state.add(userIri, Values.iri(ConnectorModels.HAS_RESOURCE), orgIri);
                    state.add(orgIri, Modeller.rdfType(), Modeller.github("Organization"));
                    state.add(orgIri, Modeller.github("name"), Values.literal(org.getName()));
                    state.add(orgIri, Modeller.github("login"), Values.literal(org.getLogin()));
                }
            }

            checkpoint = Optional.of(Checkpoints.of(state));
            status = ConnectorStatus.IDLE;
        } catch (Exception e) {
            status = ConnectorStatus.ERROR;
            state.add(connectorId, Values.iri(ConnectorModels.HAS_SYNC_STATUS), Values.literal("ERROR"));
            state.add(connectorId, Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.literal(Instant.now().toString()));
        }
    }

    private void addRepository(GHRepository repo, IRI ownerIri) {
        try {
            IRI repoIri = Modeller.toDomainURN("github:repo", repo.getFullName());
            state.add(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), repoIri);
            state.add(ownerIri, Values.iri(ConnectorModels.HAS_RESOURCE), repoIri);
            state.add(repoIri, Modeller.rdfType(), Modeller.github("Repository"));
            state.add(repoIri, Modeller.github("name"), Values.literal(repo.getName()));
            state.add(repoIri, Modeller.github("fullName"), Values.literal(repo.getFullName()));
            state.add(repoIri, Modeller.github("private"), Values.literal(repo.isPrivate()));
            state.add(repoIri, Modeller.github("forksCount"), Values.literal(repo.getForksCount()));
            state.add(repoIri, Modeller.github("stargazersCount"), Values.literal(repo.getStargazersCount()));
            state.add(repoIri, Modeller.github("openIssuesCount"), Values.literal(repo.getOpenIssueCount()));
            state.add(repoIri, Modeller.github("defaultBranch"), Values.literal(repo.getDefaultBranch()));

            repo.getBranches().forEach((name, branch) -> {
                IRI branchIri = Modeller.toDomainURN("github:branch", repo.getFullName() + ":" + name);
                state.add(repoIri, Values.iri(ConnectorModels.HAS_RESOURCE), branchIri);
                state.add(branchIri, Modeller.rdfType(), Modeller.github("Branch"));
                state.add(branchIri, Modeller.github("name"), Values.literal(name));
                state.add(branchIri, Modeller.github("protectionEnabled"), Values.literal(branch.isProtected()));
            });

            for (org.kohsuke.github.GHHook h : repo.getHooks()) {
                IRI hookIri = Modeller.toDomainURN("github:webhook", repo.getFullName() + ":" + h.getId());
                state.add(repoIri, Values.iri(ConnectorModels.HAS_CONTROL), hookIri);
                state.add(hookIri, Modeller.rdfType(), Modeller.github("WebHook"));
                if (h.getConfig() != null) {
                    String hookUrl = h.getConfig().get("url");
                    if (hookUrl != null) {
                        state.add(hookIri, Modeller.github("configUrl"), Values.literal(hookUrl));
                    }
                }
                state.add(hookIri, Modeller.github("active"), Values.literal(h.isActive()));
            }

            // Collaborators and access control
            for (GHUser collaborator : repo.listCollaborators().toList()) {
                IRI collaboratorIri = Modeller.toDomainURN("github:user", collaborator.getLogin());
                state.add(repoIri, Values.iri(ConnectorModels.HAS_USER), collaboratorIri);
                state.add(collaboratorIri, Modeller.rdfType(), Modeller.github("User"));
                state.add(collaboratorIri, Modeller.github("login"), Values.literal(collaborator.getLogin()));
            }

            // Topics/tags for repository governance visibility
            for (String topic : repo.listTopics()) {
                IRI topicIri = Modeller.toDomainURN("github:topic", repo.getFullName() + ":" + topic);
                state.add(repoIri, Values.iri(ConnectorModels.HAS_SUBSYSTEM), topicIri);
                state.add(topicIri, Modeller.rdfType(), Modeller.github("Topic"));
                state.add(topicIri, Modeller.github("name"), Values.literal(topic));
            }
        } catch (IOException e) {
            // best-effort ignore PMS and continue the sync; preserve status field metadata in outer catch
        }
    }

    private void addUser(GHUser user, IRI orgIri) {
        try {
            IRI userIri = Modeller.toDomainURN("github:user", user.getLogin());
            state.add(orgIri, Values.iri(ConnectorModels.HAS_USER), userIri);
            state.add(userIri, Modeller.rdfType(), Modeller.github("User"));
            state.add(userIri, Modeller.github("name"), Values.literal(user.getName()));
            state.add(userIri, Modeller.github("login"), Values.literal(user.getLogin()));
        } catch (IOException e) {
            // best-effort metadata extraction
        }
    }

    private void addTeam(GHTeam team, IRI orgIri) {
        IRI teamIri = Modeller.toDomainURN("github:team", orgIri.stringValue() + ":" + team.getSlug());
        state.add(orgIri, Values.iri(ConnectorModels.HAS_TEAM), teamIri);
        state.add(teamIri, Modeller.rdfType(), Modeller.github("Team"));
        state.add(teamIri, Modeller.github("name"), Values.literal(team.getName()));
        state.add(teamIri, Modeller.github("privacy"), Values.literal(team.getPrivacy()));

        try {
            for (GHUser member : team.getMembers()) {
                IRI memberIri = Modeller.toDomainURN("github:user", member.getLogin());
                state.add(teamIri, Values.iri(ConnectorModels.HAS_USER), memberIri);
                state.add(memberIri, Modeller.rdfType(), Modeller.github("User"));
                state.add(memberIri, Modeller.github("login"), Values.literal(member.getLogin()));
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
        m.add(connectorId, Modeller.connect("hasName"), Values.literal(getName()));
        m.add(connectorId, Modeller.connect("hasDescription"), Values.literal(getDescription()));
        return m;
    }
}