package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.COMMONS;


public interface IQ_NS {

public static final IRI A_WORKFLOW = Values.iri(COMMONS.IQ_NS, "Workflow");
public static final IRI hasInitialState = Values.iri(COMMONS.IQ_NS, "initial");
public static final IRI hasTransition = Values.iri(COMMONS.IQ_NS, "to");
public static final IRI hasGuard = Values.iri(COMMONS.IQ_NS, "guard");
public static final IRI hasCurrentState = Values.iri(COMMONS.IQ_NS, "state");

    public static final IRI doStart = Values.iri(COMMONS.IQ_NS, "start");
    public static final IRI doStop = Values.iri(COMMONS.IQ_NS, "stop");
}
