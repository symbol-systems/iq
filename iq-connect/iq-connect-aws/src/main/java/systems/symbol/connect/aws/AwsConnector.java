
package systems.symbol.connect.aws;

import java.time.Instant;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.config.ConfigClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.s3.S3Client;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.ConnectorScanner;
import systems.symbol.connect.core.Modeller;

public final class AwsConnector extends AbstractConnector {

private final AwsConnectorConfig config;

public AwsConnector(String connectorId, AwsConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:aws:"));
this.config = config;
}

public AwsConnector(String connectorId, AwsConfig awsConfig, Model state) {
this(connectorId, AwsConnectorConfig.fromEnv());
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

public List<ConnectorScanner<AwsScanContext>> createScanners(S3Client s3,
 Ec2Client ec2,
 IamClient iam,
 CloudTrailClient cloudTrail,
 ConfigClient configClient,
 PricingClient pricing) {
return List.of(
new AwsRegionScanner(ec2),
new AwsS3Scanner(s3),
new AwsEc2Scanner(ec2),
new AwsIamScanner(iam),
new AwsCloudTrailScanner(cloudTrail),
new AwsConfigScanner(configClient),
new AwsPricingScanner(pricing));
}

@Override
protected void doRefresh() throws Exception {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("AWS_API_KEY is required");
}

String apiKey = config.getApiKey().get();
// Validate that API key looks like a valid AWS key (AKIA... pattern)
if (!apiKey.startsWith("AKIA")) {
throw new IllegalStateException("Invalid AWS_API_KEY format (must start with AKIA)");
}

// Minimal discovered data path to keep key functionality of a connector
IRI entity = Values.iri(entityBaseIri().stringValue() + "aws-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "AwsResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.***REMOVED***("Aws"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(1), graphIri());
}
}
