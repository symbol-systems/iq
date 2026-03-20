package systems.symbol.connect.aws;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorModels;

public class AwsModellerTest {

    @Test
    void testAwsModellerWritesGraphScopedTriples() {
        Model model = new LinkedHashModel();
        IRI graph = SimpleValueFactory.getInstance().createIRI("urn:aws:test:graph");
        IRI tbox = SimpleValueFactory.getInstance().createIRI("https://example.org/aws#");
        IRI abox = SimpleValueFactory.getInstance().createIRI("urn:aws:");
        IRI connectorId = SimpleValueFactory.getInstance().createIRI("urn:aws:connector:test");

        AwsModeller modeller = new AwsModeller(model, graph, tbox, abox);

        IRI account = modeller.account(connectorId, "123456789012", "arn:aws:iam::123456789012:root");
        IRI bucket = modeller.s3Bucket(connectorId, "my-bucket");

        assertTrue(model.contains(connectorId, Values.iri(ConnectorModels.HAS_ACCOUNT), account, graph));
        assertTrue(model.contains(account, Values.iri("https://example.org/aws#accountId"), Values.literal("123456789012"), graph));
        assertTrue(model.contains(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), bucket, graph));
        assertTrue(model.contains(bucket, Values.iri("https://example.org/aws#name"), Values.literal("my-bucket"), graph));
    }
}
