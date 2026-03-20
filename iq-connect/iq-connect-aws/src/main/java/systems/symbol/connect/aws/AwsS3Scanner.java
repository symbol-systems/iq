package systems.symbol.connect.aws;

import systems.symbol.connect.core.ConnectorScanner;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

final class AwsS3Scanner implements ConnectorScanner<AwsScanContext> {

    private final S3Client s3;

    AwsS3Scanner(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public void scan(AwsScanContext context) {
        for (Bucket bucket : s3.listBuckets().buckets()) {
            context.ensureBucket(bucket.name());
        }
    }
}