package systems.symbol.connect.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.junit.jupiter.api.Test;

import systems.symbol.connect.core.ConnectorScanner;

public class AwsConnectorScannerCompositionTest {

    @Test
    void testAwsScannerCompositionOrder() {
        AwsConnector connector = new AwsConnector("urn:aws:connector:test", new AwsConfig("us-east-1"), new LinkedHashModel());

        List<ConnectorScanner<AwsScanContext>> scanners = connector.createScanners(
            null,
            null,
            null,
            null,
            null,
            null);

        assertEquals(7, scanners.size());
        assertEquals(AwsRegionScanner.class, scanners.get(0).getClass());
        assertEquals(AwsS3Scanner.class, scanners.get(1).getClass());
        assertEquals(AwsEc2Scanner.class, scanners.get(2).getClass());
        assertEquals(AwsIamScanner.class, scanners.get(3).getClass());
        assertEquals(AwsCloudTrailScanner.class, scanners.get(4).getClass());
        assertEquals(AwsConfigScanner.class, scanners.get(5).getClass());
        assertEquals(AwsPricingScanner.class, scanners.get(6).getClass());
    }
}