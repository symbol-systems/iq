package systems.symbol.agent;

import org.eclipse.rdf4j.model.Resource;
import systems.symbol.decide.I_Delegate;
import systems.symbol.intent.I_Intent;
import systems.symbol.platform.I_Self;

public interface I_Selfie extends I_Agent, I_Self, I_Intent, I_Delegate<Resource> {
}
