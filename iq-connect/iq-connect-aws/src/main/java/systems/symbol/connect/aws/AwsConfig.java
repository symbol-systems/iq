package systems.symbol.connect.aws;

import java.util.Optional;

/**
 * Simple config holder for the AWS connector.
 */
public final class AwsConfig {

private final String region;

public AwsConfig(String region) {
this.region = region;
}

public Optional<String> getRegion() {
return Optional.ofNullable(region);
}
}
