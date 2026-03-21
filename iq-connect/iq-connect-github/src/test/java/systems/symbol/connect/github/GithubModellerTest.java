package systems.symbol.connect.github;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorModels;

public class GithubModellerTest {

@Test
void testGithubModellerWritesGraphScopedTriples() {
Model model = new LinkedHashModel();
IRI graph = SimpleValueFactory.getInstance().createIRI("urn:github:test:graph");
IRI tbox = SimpleValueFactory.getInstance().createIRI("https://example.org/github#");
IRI abox = SimpleValueFactory.getInstance().createIRI("urn:github:");
IRI connectorId = SimpleValueFactory.getInstance().createIRI("urn:github:connector:test");

GithubModeller modeller = new GithubModeller(model, graph, tbox, abox);

IRI orgIri = modeller.organization(connectorId, "my-org", "My Org");
IRI userIri = modeller.rootUser(connectorId, "octocat", "The Octocat");
IRI repoIri = modeller.repository(connectorId, orgIri, "repo", "my-org/repo", false, 0, 0, 0, "main");
IRI branchIri = modeller.branch(repoIri, "my-org/repo", "main", true);
IRI hookIri = modeller.webHook(repoIri, "my-org/repo", 123L, "https://example.com/webhook", true);

assertTrue(model.contains(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), orgIri, graph));
assertTrue(model.contains(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), userIri, graph));
assertTrue(model.contains(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), repoIri, graph));
assertTrue(model.contains(repoIri, Values.iri(ConnectorModels.HAS_RESOURCE), branchIri, graph));
assertTrue(model.contains(repoIri, Values.iri(ConnectorModels.HAS_CONTROL), hookIri, graph));
assertTrue(model.contains(repoIri, Values.iri("https://example.org/github#name"), Values.***REMOVED***("repo"), graph));
}
}
