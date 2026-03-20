package systems.symbol.connect.aws;

import systems.symbol.connect.core.ConnectorScanner;

import software.amazon.awssdk.services.ec2.Ec2Client;

final class AwsRegionScanner implements ConnectorScanner<AwsScanContext> {

private final Ec2Client ec2;

AwsRegionScanner(Ec2Client ec2) {
this.ec2 = ec2;
}

@Override
public void scan(AwsScanContext context) {
context.ensureConfiguredRegion();
for (software.amazon.awssdk.services.ec2.model.Region awsRegion : ec2.describeRegions().regions()) {
context.ensureRegion(awsRegion.regionName(), awsRegion.endpoint());
}
}
}