package systems.symbol.kernel.policy;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.event.KernelEvent;

public final class PolicyDecisionEvent {

private final PolicyInput input;
private final PolicyResult result;

public PolicyDecisionEvent(PolicyInput input, PolicyResult result) {
this.input = input;
this.result = result;
}

public PolicyInput getInput() {
return input;
}

public PolicyResult getResult() {
return result;
}

public KernelEvent toKernelEvent(IRI source) {
IRI topic = result.allowed() ? PolicyVocab.EVENT_POLICY_EVALUATED : PolicyVocab.EVENT_POLICY_DENIED;
return KernelEvent.on(topic)
.source(source)
.payload(this)
.build();
}
}
