package systems.symbol.connect.aws;

import org.eclipse.rdf4j.model.IRI;

import software.amazon.awssdk.services.config.ConfigClient;
import software.amazon.awssdk.services.config.model.ConfigurationRecorder;

final class AwsConfigScanner {

private AwsConfigScanner() {
}

static void scan(ConfigClient configClient, AwsScanContext context) {
AwsModeller modeller = context.modeller();
IRI connectorId = context.connectorId();
IRI accountIri = context.accountIri();
IRI defaultRegionIri = context.ensureConfiguredRegion();

IRI configRecorderIri = null;
int recorderCount = 0;
for (ConfigurationRecorder recorder : configClient.describeConfigurationRecorders().configurationRecorders()) {
recorderCount++;
IRI discoveredRecorderIri = modeller.configRecorder(connectorId, accountIri, defaultRegionIri, recorder.name());
if (recorderCount == 1) {
configRecorderIri = discoveredRecorderIri;
} else {
configRecorderIri = null;
}
}

String configRulesNextToken = null;
do {
var request = software.amazon.awssdk.services.config.model.DescribeConfigRulesRequest.builder();
if (configRulesNextToken != null && !configRulesNextToken.isBlank()) {
request.nextToken(configRulesNextToken);
}

var configRules = configClient.describeConfigRules(request.build());
for (software.amazon.awssdk.services.config.model.ConfigRule rule : configRules.configRules()) {
String sourceOwner = rule.source() != null ? rule.source().ownerAsString() : null;
modeller.configRule(connectorId,
accountIri,
defaultRegionIri,
configRecorderIri,
rule.configRuleName(),
sourceOwner);
}
configRulesNextToken = configRules.nextToken();
} while (configRulesNextToken != null && !configRulesNextToken.isBlank());
}
}