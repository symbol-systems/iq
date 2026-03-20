package systems.symbol.connect.github;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

import systems.symbol.connect.core.ConnectorGraphModeller;
import systems.symbol.connect.core.ConnectorModels;

/**
 * GitHub graph modeller that maps discovered GitHub resources into connector RDF state.
 */
public final class GithubModeller extends ConnectorGraphModeller {

public GithubModeller(Model model, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
super(model, graphIri, ontologyBaseIri, entityBaseIri);
}

public IRI organization(IRI connectorId, String login, String name) {
IRI orgIri = organization(login, name);
link(connectorId, ConnectorModels.HAS_RESOURCE, orgIri);
return orgIri;
}

public IRI organization(String login, String name) {
IRI orgIri = entity("org", login);
addType(orgIri, "Organization");
addLiteral(orgIri, "name", name);
addLiteral(orgIri, "login", login);
return orgIri;
}

public IRI rootUser(IRI connectorId, String login, String name) {
IRI userIri = user(login, name);
link(connectorId, ConnectorModels.HAS_RESOURCE, userIri);
return userIri;
}

public IRI user(String login, String name) {
IRI userIri = entity("user", login);
addType(userIri, "User");
addLiteral(userIri, "name", name);
addLiteral(userIri, "login", login);
return userIri;
}

public void linkResource(Resource parent, Resource resource) {
link(parent, ConnectorModels.HAS_RESOURCE, resource);
}

public IRI repository(IRI connectorId,
  IRI ownerIri,
  String name,
  String fullName,
  boolean isPrivate,
  int forksCount,
  int stargazersCount,
  int openIssuesCount,
  String defaultBranch) {
IRI repoIri = entity("repo", fullName);
link(connectorId, ConnectorModels.HAS_RESOURCE, repoIri);
link(ownerIri, ConnectorModels.HAS_RESOURCE, repoIri);
addType(repoIri, "Repository");
addLiteral(repoIri, "name", name);
addLiteral(repoIri, "fullName", fullName);
addLiteral(repoIri, "private", isPrivate);
addLiteral(repoIri, "forksCount", forksCount);
addLiteral(repoIri, "stargazersCount", stargazersCount);
addLiteral(repoIri, "openIssuesCount", openIssuesCount);
addLiteral(repoIri, "defaultBranch", defaultBranch);
return repoIri;
}

public IRI branch(IRI repoIri, String repoFullName, String branchName, boolean protectionEnabled) {
IRI branchIri = entity("branch", repoFullName + ":" + branchName);
link(repoIri, ConnectorModels.HAS_RESOURCE, branchIri);
addType(branchIri, "Branch");
addLiteral(branchIri, "name", branchName);
addLiteral(branchIri, "protectionEnabled", protectionEnabled);
return branchIri;
}

public IRI webHook(IRI repoIri, String repoFullName, long hookId, String configUrl, boolean active) {
IRI hookIri = entity("webhook", repoFullName + ":" + hookId);
link(repoIri, ConnectorModels.HAS_CONTROL, hookIri);
addType(hookIri, "WebHook");
addLiteral(hookIri, "configUrl", configUrl);
addLiteral(hookIri, "active", active);
return hookIri;
}

public IRI collaborator(IRI repoIri, String login) {
IRI collaboratorIri = user(login, null);
link(repoIri, ConnectorModels.HAS_USER, collaboratorIri);
return collaboratorIri;
}

public IRI topic(IRI repoIri, String repoFullName, String topicName) {
IRI topicIri = entity("topic", repoFullName + ":" + topicName);
link(repoIri, ConnectorModels.HAS_SUBSYSTEM, topicIri);
addType(topicIri, "Topic");
addLiteral(topicIri, "name", topicName);
return topicIri;
}

public IRI orgUser(IRI orgIri, String login, String name) {
IRI userIri = user(login, name);
link(orgIri, ConnectorModels.HAS_USER, userIri);
return userIri;
}

public IRI team(IRI orgIri, String teamKey, String name, String privacy) {
IRI teamIri = entity("team", teamKey);
link(orgIri, ConnectorModels.HAS_TEAM, teamIri);
addType(teamIri, "Team");
addLiteral(teamIri, "name", name);
addLiteral(teamIri, "privacy", privacy);
return teamIri;
}

public IRI teamMember(IRI teamIri, String login) {
IRI memberIri = user(login, null);
link(teamIri, ConnectorModels.HAS_USER, memberIri);
return memberIri;
}
}
