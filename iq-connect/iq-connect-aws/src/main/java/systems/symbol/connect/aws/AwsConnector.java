package systems.symbol.connect.aws;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.Modeller;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.config.ConfigClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorScanner;

/**
 * Example AWS connector implementation.
 *
 * <p>This connector is intended as a complete implementation showing how to
 * integrate AWS SDK calls with the IQ connector model and writes metadata into
 * the connector state model.
 */
public final class AwsConnector extends AbstractConnector {

private static final String DEFAULT_REGION = "us-east-1";

private final AwsConfig config;

public AwsConnector(String connectorId, AwsConfig config, Model state) {
this(connectorId,
config,
state,
Values.iri(connectorId + "/graph/current"),
Values.iri(Modeller.getAwsOntology()),
Values.iri("urn:aws:"));
}

public AwsConnector(String connectorId, AwsConfig config, Model state, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
super(connectorId, state, graphIri, ontologyBaseIri, entityBaseIri);
this.config = config;
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_WRITE;
}

@Override
protected void doRefresh() throws Exception {
IRI connectorId = getConnectorId();
Region region = Region.of(config.getRegion().orElse(DEFAULT_REGION));
AwsModeller modeller = new AwsModeller(getModel(), graphIri(), ontologyBaseIri(), entityBaseIri());

try (S3Client s3 = S3Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
 StsClient sts = StsClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
 Ec2Client ec2 = Ec2Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
 IamClient iam = IamClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
 CloudTrailClient cloudTrail = CloudTrailClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
 ConfigClient configClient = ConfigClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
 PricingClient pricing = PricingClient.builder().region(Region.US_EAST_1).credentialsProvider(DefaultCredentialsProvider.create()).build()) {

GetCallerIdentityResponse identity = sts.getCallerIdentity();
IRI accountIri = modeller.account(connectorId, identity.account(), identity.arn());
AwsScanContext context = new AwsScanContext(connectorId, accountIri, region, modeller);

List<ConnectorScanner<AwsScanContext>> scanners = createScanners(s3, ec2, iam, cloudTrail, configClient, pricing);

for (ConnectorScanner<AwsScanContext> scanner : scanners) {
scanner.scan(context);
}
}
}

List<ConnectorScanner<AwsScanContext>> createScanners(S3Client s3,
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
}
