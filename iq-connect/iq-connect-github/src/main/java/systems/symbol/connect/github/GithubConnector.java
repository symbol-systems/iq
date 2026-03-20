package systems.symbol.connect.github;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.Modeller;

import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
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
GitHub github = GitHub.connectUsingOAuth(config.getAccessToken());
GithubModeller modeller = new GithubModeller(getModel(), graphIri(), ontologyBaseIri(), entityBaseIri());
GithubScanContext context = new GithubScanContext(getConnectorId(), modeller);

if (config.getOrganization().isPresent()) {
GHOrganization organization = github.getOrganization(config.getOrganization().get());
GithubOrganizationScanner.scan(organization, context);
} else {
GHMyself me = github.getMyself();
GithubMyselfScanner.scan(me, context);
}
}
}