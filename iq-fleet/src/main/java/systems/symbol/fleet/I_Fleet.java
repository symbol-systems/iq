package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.agent.I_Agent;
import systems.symbol.platform.I_StartStop;

import java.util.Collection;

public interface I_Fleet extends I_StartStop {

//Set<IRI> deploy(Model agents, I_Secrets secrets) throws SecretsException, StateException;
I_Agent getAgent(IRI agent);
Collection<I_Agent> getAgents();


void start(IRI agent) throws Exception;
void stop(IRI agent) throws Exception;

}
