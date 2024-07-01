package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/**
 * The {@code IQ_NS} interface provides constants representing well-known IRIs within IQ.
 * These IRIs are used for defining various aspects of symbolic cognition such as workflows,
 * steps, guards, states, actions and LLM prompts.
 */
public interface IQ_NS {
String IQ = "iq:"; // reserved for internal use
String AI = "urn:ai:"; // runtime foundational knowledge
//String MY = "urn:my:"; // domain knowledge
String TEST = "iq:test:"; // reserved for testing

// Workflow-related IRIs
IRI A_WORKFLOW = Values.iri(IQ, "Workflow");
IRI initialStep = Values.iri(IQ, "initial");
IRI nextStep = Values.iri(IQ, "to");
IRI hasGuard = Values.iri(IQ, "guard");
IRI hasCurrentState = Values.iri(IQ, "state");

// Action-related IRIs
IRI doBoot = Values.iri(IQ, "boot");
IRI KNOWS = Values.iri(IQ, "knows");
IRI FORGOT = Values.iri(IQ_NS.IQ+"forgot");
IRI TRUSTS = Values.iri(IQ, "trusts");;
IRI PROMPT = Values.iri(IQ, "prompt");
IRI NAME = Values.iri(IQ, "name");;

// Miscellaneous IRIs
IRI nop = Values.iri(IQ, "nop");
}
