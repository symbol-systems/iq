package systems.symbol.connect.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

public class ConnectorProvenanceTest {

    @Test
    void testSyncLifecycleProvenance() {
        Model state = new LinkedHashModel();
        IRI connector = SimpleValueFactory.getInstance().createIRI("urn:connector:unit-test");

        IRI activity = ConnectorProvenance.markSyncStarted(state, connector);

        assertTrue(state.contains(activity, RDF.TYPE, PROV.ACTIVITY));
        assertTrue(state.contains(activity, PROV.WAS_ASSOCIATED_WITH, connector));
        assertTrue(state.contains(activity, PROV.STARTED_AT_TIME, null));
        assertTrue(state.contains(connector, PROV.WAS_GENERATED_BY, activity));

        ConnectorProvenance.markSyncCompleted(state, activity);
        assertTrue(state.contains(activity, PROV.ENDED_AT_TIME, null));

        Throwable error = new RuntimeException("sync failed");
        ConnectorProvenance.markSyncFailed(state, activity, error);
        assertTrue(state.contains(activity, null, SimpleValueFactory.getInstance().createLiteral("sync failed")));
    }
}
