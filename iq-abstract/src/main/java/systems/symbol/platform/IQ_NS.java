package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/**
 * The {@code IQ_NS} interface provides constants representing well-known IRIs
 * within IQ.
 * These IRIs are used for defining various aspects of symbolic cognition such
 * as workflows,
 * steps, guards, states, actions and LLM prompts.
 */
public interface IQ_NS {
String IQ = "iq:"; // reserved for internal use
String AI = "ai:"; // runtime foundational knowledge
String TEST = "iq:test:"; // reserved for testing

// Workflow-related IRIs
IRI hasInitialState = Values.iri(IQ, "initial");
IRI TO = Values.iri(IQ, "to");
IRI hasGuard = Values.iri(IQ, "guard");
IRI hasCurrentState = Values.iri(IQ, "state");

// Action-related IRIs
IRI KNOWS = Values.iri(IQ, "knows");
IRI FORGOT = Values.iri(IQ_NS.IQ + "forgot");
IRI TRUSTS = Values.iri(IQ, "trusts");
IRI SECRET = Values.iri(IQ, "secret");
IRI OOPS = Values.iri(IQ, "oops");
IRI NEEDS = Values.iri(IQ, "needs");
IRI NAME = Values.iri(IQ, "name");
}
