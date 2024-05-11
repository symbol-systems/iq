package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import static systems.symbol.COMMONS.IQ_NS;


public interface IQ_NS {
IRI A_WORKFLOW = Values.iri(IQ_NS, "Workflow");
IRI initialStep = Values.iri(IQ_NS, "initial");
IRI nextStep = Values.iri(IQ_NS, "to");
IRI hasGuard = Values.iri(IQ_NS, "guard");
IRI hasCurrentState = Values.iri(IQ_NS, "state");

IRI doBoot = Values.iri(IQ_NS, "boot");
IRI KNOWS = Values.iri(IQ_NS, "knows");
IRI PROMPT = Values.iri(IQ_NS, "prompt");

IRI nop = Values.iri(IQ_NS, "nop");

}
