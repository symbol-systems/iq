package systems.symbol.connect.aws;

import org.eclipse.rdf4j.model.IRI;

import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.DescribeTrailsRequest;
import software.amazon.awssdk.services.cloudtrail.model.Trail;

final class AwsCloudTrailScanner {

private AwsCloudTrailScanner() {
}

static void scan(CloudTrailClient cloudTrail, AwsScanContext context) {
AwsModeller modeller = context.modeller();
IRI connectorId = context.connectorId();
IRI accountIri = context.accountIri();

var trailsResponse = cloudTrail.describeTrails(
DescribeTrailsRequest.builder()
.includeShadowTrails(true)
.build());

for (Trail trail : trailsResponse.trailList()) {
IRI trailRegionIri = context.ensureRegion(trail.homeRegion(), null);
IRI trailBucketIri = context.ensureBucket(trail.s3BucketName());
modeller.cloudTrail(connectorId, accountIri, trailRegionIri, trail.name(), trailBucketIri, trail.s3BucketName());
}
}
}