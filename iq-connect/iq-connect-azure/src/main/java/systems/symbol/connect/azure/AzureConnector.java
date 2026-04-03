
package systems.symbol.connect.azure;

import java.time.Instant;
import java.util.Locale;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.GenericResource;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.Modeller;

public final class AzureConnector extends AbstractConnector {

private final AzureConnectorConfig config;

public AzureConnector(String connectorId, AzureConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:azure:"));
this.config = config;
}

@Override
public ConnectorMode getMode() {
return ConnectorMode.READ_ONLY;
}

@Override
protected void doRefresh() throws Exception {
String subscriptionId = config.getSubscriptionId().orElseThrow(() -> new IllegalStateException("AZURE_SUBSCRIPTION_ID is required"));
String tenantId = config.getTenantId().orElseGet(() -> System.getenv("AZURE_TENANT_ID"));

TokenCredential credential = new DefaultAzureCredentialBuilder().build();
AzureProfile profile = new AzureProfile(tenantId == null ? "common" : tenantId, subscriptionId, AzureEnvironment.AZURE);

AzureResourceManager resourceManager = AzureResourceManager
.authenticate(credential, profile)
.withSubscription(subscriptionId);

int resourceCount = 0;
for (GenericResource resource : resourceManager.genericResources().list()) {
if (resource.id() == null) {
continue;
}

IRI entity = entity("azure", resource.id());
addType(entity, "AzureResource");
addLiteral(entity, "name", resource.name());
addLiteral(entity, "resourceType", resource.resourceType());
addLiteral(entity, "resourceGroup", resource.resourceGroupName());
addLiteral(entity, "subscriptionId", resource.subscriptionId());

getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
resourceCount++;
}

getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(resourceCount), graphIri());
}
}

