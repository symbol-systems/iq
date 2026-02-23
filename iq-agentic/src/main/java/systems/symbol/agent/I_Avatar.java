package systems.symbol.agent;

import org.eclipse.rdf4j.model.Resource;

import systems.symbol.decide.I_Delegate;
import systems.symbol.intent.I_Intent;

public interface I_Avatar extends I_Agent, I_Intent, I_Delegate<Resource> {
}
