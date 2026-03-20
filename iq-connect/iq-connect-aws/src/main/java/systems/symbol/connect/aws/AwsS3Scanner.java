package systems.symbol.connect.aws;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

final class AwsS3Scanner {

    private AwsS3Scanner() {
    }

    static void scan(S3Client s3, AwsScanContext context) {
        for (Bucket bucket : s3.listBuckets().buckets()) {
            context.ensureBucket(bucket.name());
        }
    }
}