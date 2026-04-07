package systems.symbol.connect.aws;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
IRI region = modeller.region(connectorId, account, "us-east-1", "ec2.us-east-1.amazonaws.com");
IRI bucket = modeller.s3Bucket(connectorId, account, region, "my-bucket");
IRI trail = modeller.cloudTrail(connectorId, account, region, "my-trail", bucket, "my-bucket");
IRI pricingService = modeller.pricingService(connectorId, "AmazonEC2", List.of("instanceType", "regionCode"));

assertTrue(model.contains(connectorId, Values.iri(ConnectorModels.HAS_ACCOUNT), account, graph));
assertTrue(model.contains(account, Values.iri("https://example.org/aws#accountId"), Values.literal("123456789012"), graph));
assertTrue(model.contains(connectorId, Values.iri(ConnectorModels.HAS_RESOURCE), bucket, graph));
assertTrue(model.contains(bucket, Values.iri("https://example.org/aws#name"), Values.literal("my-bucket"), graph));
assertTrue(model.contains(bucket, Values.iri("https://example.org/aws#inAccount"), account, graph));
assertTrue(model.contains(bucket, Values.iri("https://example.org/aws#inRegion"), region, graph));
assertTrue(model.contains(trail, Values.iri("https://example.org/aws#logsToBucket"), bucket, graph));
assertTrue(model.contains(pricingService, Values.iri("https://example.org/aws#hasAttribute"), null, graph));
}
}
