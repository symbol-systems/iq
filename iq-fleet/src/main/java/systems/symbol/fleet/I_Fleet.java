package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.I_StartStop;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import java.util.Collection;
import java.util.Set;

public interface I_Fleet extends I_StartStop {

//    Set<IRI> deploy(Model agents, I_Secrets secrets) throws SecretsException, StateException;
    I_Agent getAgent(IRI agent);
    Collection<I_Agent> getAgents();


    I_Agent start(IRI agent) throws Exception;
    Resource stop(IRI agent) throws Exception;

}
