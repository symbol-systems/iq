package systems.symbol.connect.aws;

import software.amazon.awssdk.services.ec2.Ec2Client;

final class AwsRegionScanner {

    private AwsRegionScanner() {
    }

    static void scan(Ec2Client ec2, AwsScanContext context) {
        context.ensureConfiguredRegion();
        for (software.amazon.awssdk.services.ec2.model.Region awsRegion : ec2.describeRegions().regions()) {
            context.ensureRegion(awsRegion.regionName(), awsRegion.endpoint());
        }
    }
}