
package systems.symbol.connect.aws;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.config.ConfigClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.s3.S3Client;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorCheckpoint;
import systems.symbol.connect.core.ConnectorErrorHandler;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.ConnectorScanner;
import systems.symbol.connect.core.ConnectorState;
import systems.symbol.connect.core.Modeller;

public final class AwsConnector extends AbstractConnector {

private final AwsConnectorConfig config;
private static final Logger log = LoggerFactory.getLogger(AwsConnector.class);

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

// Initialize connector framework components
ConnectorState state = ConnectorState.start(getConnectorId().stringValue());
ConnectorErrorHandler errorHandler = ConnectorErrorHandler.forConnector(getConnectorId().stringValue());

// Register error callbacks
errorHandler.on("error", err -> log.error("AWS sync error: {} for item {}", err.message, err.itemId));
errorHandler.on("retry", err -> log.warn("Retrying AWS item: {}", err.itemId));
errorHandler.on("dlq", err -> log.error("AWS dead-letter: {}", err.itemId));

try {
// Discover AWS resources
int resourceCount = 0;

// Add placeholder resources with framework tracking
IRI entity = Values.iri(entityBaseIri().stringValue() + "aws-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "AwsResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.***REMOVED***("Aws"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());

state.recordSuccess();
resourceCount = 1;

// Update connector metadata
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), 
  Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), 
  Values.***REMOVED***(resourceCount), graphIri());

var stats = state.finish();
log.info("AWS connector sync completed: {}", stats);
} catch (Exception ex) {
state.recordFailure("aws-sync", ex.getMessage());
errorHandler.recordError("aws-sync", ex);
var stats = state.finish();
log.error("AWS connector sync failed: {}", stats, ex);
throw ex;
}
}
}
