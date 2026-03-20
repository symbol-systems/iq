package systems.symbol.connect.core;

import java.time.Instant;
import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Utilities for writing PROV-O statements into connector state models.
 */
public final class ConnectorProvenance {

public static final String CONNECTOR_PROV_PREFIX = "https://symbol.systems/v0/connect/prov#";

private ConnectorProvenance() {
// util
}

/**
 * Starts a synchronization activity for the given connector in the model.
 */
public static IRI markSyncStarted(Model model, IRI connector) {
return markSyncStarted(model, connector, new Resource[0]);
}

/**
 * Starts a synchronization activity for the given connector in the model scoped to the given contexts.
 */
public static IRI markSyncStarted(Model model, IRI connector, Resource... contexts) {
ValueFactory vf = SimpleValueFactory.getInstance();
IRI activity = vf.createIRI(connector.stringValue() + "/sync/" + UUID.randomUUID());

model.add(activity, RDF.TYPE, PROV.ACTIVITY, contexts);
model.add(activity, PROV.WAS_ASSOCIATED_WITH, connector, contexts);
model.add(activity, PROV.WAS_INFORMED_BY, connector, contexts);
model.add(activity, PROV.STARTED_AT_TIME, vf.createLiteral(Instant.now().toString(), XSD.DATETIME), contexts);

model.add(connector, PROV.WAS_GENERATED_BY, activity, contexts);

return activity;
}

/**
 * Marks a synchronization activity as successful.
 */
public static void markSyncCompleted(Model model, IRI activity) {
markSyncCompleted(model, activity, new Resource[0]);
}

/**
 * Marks a synchronization activity as successful in the given contexts.
 */
public static void markSyncCompleted(Model model, IRI activity, Resource... contexts) {
ValueFactory vf = SimpleValueFactory.getInstance();

model.add(activity, PROV.ENDED_AT_TIME, vf.createLiteral(Instant.now().toString(), XSD.DATETIME), contexts);
}

/**
 * Marks a synchronization activity as failed with an error message.
 */
public static void markSyncFailed(Model model, IRI activity, Throwable error) {
markSyncFailed(model, activity, error, new Resource[0]);
}

/**
 * Marks a synchronization activity as failed with an error message in the given contexts.
 */
public static void markSyncFailed(Model model, IRI activity, Throwable error, Resource... contexts) {
ValueFactory vf = SimpleValueFactory.getInstance();
IRI failureMessage = vf.createIRI(CONNECTOR_PROV_PREFIX + "failureMessage");

model.add(activity, PROV.ENDED_AT_TIME, vf.createLiteral(Instant.now().toString(), XSD.DATETIME), contexts);
model.add(activity, failureMessage, vf.createLiteral(error.getMessage()), contexts);
}

}
