package systems.symbol.connect.aws;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;

import software.amazon.awssdk.regions.Region;

final class AwsScanContext {

    private final IRI connectorId;
    private final IRI accountIri;
    private final Region configuredRegion;
    private final AwsModeller modeller;

    private final Map<String, IRI> regionIrisById = new HashMap<>();
    private final Map<String, IRI> bucketIrisByName = new HashMap<>();

    AwsScanContext(IRI connectorId, IRI accountIri, Region configuredRegion, AwsModeller modeller) {
        this.connectorId = connectorId;
        this.accountIri = accountIri;
        this.configuredRegion = configuredRegion;
        this.modeller = modeller;
    }

    IRI connectorId() {
        return connectorId;
    }

    IRI accountIri() {
        return accountIri;
    }

    AwsModeller modeller() {
        return modeller;
    }

    IRI ensureConfiguredRegion() {
        return ensureRegion(configuredRegion.id(), null);
    }

    IRI ensureRegion(String regionId, String endpoint) {
        if (regionId == null || regionId.isBlank()) {
            return null;
        }
        return regionIrisById.computeIfAbsent(regionId, key -> modeller.region(connectorId, accountIri, key, endpoint));
    }

    IRI ensureBucket(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            return null;
        }
        return bucketIrisByName.computeIfAbsent(bucketName, key -> modeller.s3Bucket(connectorId, accountIri, null, key));
    }

    IRI regionForAvailabilityZone(String availabilityZone) {
        String regionId = regionFromAvailabilityZone(availabilityZone).orElse(configuredRegion.id());
        return ensureRegion(regionId, null);
    }

    private Optional<String> regionFromAvailabilityZone(String availabilityZone) {
        if (availabilityZone == null || availabilityZone.isBlank()) {
            return Optional.empty();
        }

        char last = availabilityZone.charAt(availabilityZone.length() - 1);
        if (Character.isLetter(last) && availabilityZone.length() > 1) {
            return Optional.of(availabilityZone.substring(0, availabilityZone.length() - 1));
        }
        return Optional.of(availabilityZone);
    }
}