package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

/**
 * Base graph modeller with reusable connector-edge helpers.
 */
public abstract class AbstractConnectorModeller extends ConnectorGraphModeller {

protected AbstractConnectorModeller(Model model, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
super(model, graphIri, ontologyBaseIri, entityBaseIri);
}

protected final void linkConnectorAccount(Resource connectorId, Resource accountIri) {
link(connectorId, ConnectorModels.HAS_ACCOUNT, accountIri);
}

protected final void linkConnectorRegion(Resource connectorId, Resource regionIri) {
link(connectorId, ConnectorModels.HAS_REGION, regionIri);
}

protected final void linkConnectorResource(Resource connectorId, Resource resourceIri) {
link(connectorId, ConnectorModels.HAS_RESOURCE, resourceIri);
}

protected final void linkConnectorSubsystem(Resource connectorId, Resource subsystemIri) {
link(connectorId, ConnectorModels.HAS_SUBSYSTEM, subsystemIri);
}

protected final void linkConnectorRole(Resource connectorId, Resource roleIri) {
link(connectorId, ConnectorModels.HAS_ROLE, roleIri);
}

protected final void linkConnectorUser(Resource connectorId, Resource userIri) {
link(connectorId, ConnectorModels.HAS_USER, userIri);
}

protected final void linkConnectorTeam(Resource connectorId, Resource teamIri) {
link(connectorId, ConnectorModels.HAS_TEAM, teamIri);
}

protected final void linkConnectorPolicy(Resource connectorId, Resource policyIri) {
link(connectorId, ConnectorModels.HAS_POLICY, policyIri);
}

protected final void linkConnectorControl(Resource connectorId, Resource controlIri) {
link(connectorId, ConnectorModels.HAS_CONTROL, controlIri);
}

protected final void linkInOntology(Resource subject, String predicateLocalName, Resource object) {
if (subject == null || object == null) {
return;
}
add(subject, ontology(predicateLocalName), object);
}
}