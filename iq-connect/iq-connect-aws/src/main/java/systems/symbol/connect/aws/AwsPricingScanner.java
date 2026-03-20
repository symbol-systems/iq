package systems.symbol.connect.aws;

import org.eclipse.rdf4j.model.IRI;

import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.DescribeServicesRequest;
import software.amazon.awssdk.services.pricing.model.DescribeServicesResponse;
import software.amazon.awssdk.services.pricing.model.Service;

final class AwsPricingScanner {

    private AwsPricingScanner() {
    }

    static void scan(PricingClient pricing, AwsScanContext context) {
        AwsModeller modeller = context.modeller();
        IRI connectorId = context.connectorId();

        String pricingNextToken = null;
        do {
            DescribeServicesRequest.Builder request = DescribeServicesRequest.builder();
            if (pricingNextToken != null && !pricingNextToken.isBlank()) {
                request.nextToken(pricingNextToken);
            }

            DescribeServicesResponse pricingServicesResponse = pricing.describeServices(request.build());
            for (Service pricingService : pricingServicesResponse.services()) {
                modeller.pricingService(connectorId, pricingService.serviceCode(), pricingService.attributeNames());
            }
            pricingNextToken = pricingServicesResponse.nextToken();
        } while (pricingNextToken != null && !pricingNextToken.isBlank());
    }
}