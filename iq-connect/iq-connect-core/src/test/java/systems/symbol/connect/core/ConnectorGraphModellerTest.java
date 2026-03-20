package systems.symbol.connect.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

public class ConnectorGraphModellerTest {

@Test
void testGraphAwareEntityAndOntologyWrites() {
Model model = new LinkedHashModel();
IRI graph = SimpleValueFactory.getInstance().createIRI("urn:test:graph");
IRI tbox = SimpleValueFactory.getInstance().createIRI("https://example.org/aws#");
IRI abox = SimpleValueFactory.getInstance().createIRI("urn:test:");

TestModeller modeller = new TestModeller(model, graph, tbox, abox);
IRI subject = modeller.createUser("john.doe+admin@example.org");

assertEquals("urn:test:user:john.doe%2Badmin%40example.org", subject.stringValue());
assertTrue(model.contains(subject, Modeller.rdfType(), SimpleValueFactory.getInstance().createIRI("https://example.org/aws#IAMUser"), graph));
assertTrue(model.contains(subject, SimpleValueFactory.getInstance().createIRI("https://example.org/aws#userName"), SimpleValueFactory.getInstance().createLiteral("john.doe+admin@example.org"), graph));
}

private static final class TestModeller extends ConnectorGraphModeller {

private TestModeller(Model model, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
super(model, graphIri, ontologyBaseIri, entityBaseIri);
}

private IRI createUser(String userName) {
IRI user = entity("user", userName);
addType(user, "IAMUser");
addLiteral(user, "userName", userName);
return user;
}
}
}
