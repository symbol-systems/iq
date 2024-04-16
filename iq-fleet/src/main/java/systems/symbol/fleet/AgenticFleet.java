package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.I_StateListener;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.JSR233;
import systems.symbol.rdf4j.iq.IQ_NS;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AgenticFleet implements I_Fleet {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    Map<IRI, I_Agent> agents = new HashMap<>();

    public AgenticFleet(Model fleet, I_Secrets secrets) throws StateException {
        deploy(fleet, secrets);
    }

    public void deploy(Model fleet, I_Secrets secrets) throws StateException {
        Iterable<Statement> found = fleet.getStatements(null, IQ_NS.hasInitialState, null);
        for(Statement s: found) {
            IRI self = (IRI) s.getSubject();
            I_Agent agent = newAgent(self, fleet, secrets);
            agents.put(self, agent);
            log.info("fleet.deploy.agent: {}", self);
        }
        log.info("fleet.deployed: {}", agents.keySet());
    }

    public I_Agent newAgent(IRI self, Model fleet, I_Secrets secrets) throws StateException {
        return new ExecutiveAgent(fleet, self, secrets, new JSR233(self, fleet));
    }


    @Override
    public I_Agent getAgent(IRI agent) {
        return agents.get(agent);
    }

    public Collection<I_Agent> getAgents() {
        return agents.values();
    }

    @Override
    public void start() throws Exception {
        Set<IRI> iris = agents.keySet();
        for(IRI agent: iris) {
            start(agent);
        }
    }

    @Override
    public void stop() throws Exception {
        for(IRI agent: agents.keySet()) {
            stop(agent);
        }
    }

    @Override
    public I_Agent start(IRI self) throws Exception {
        I_Agent agent = getAgent(self);
        if (agent==null) return null;
        agent.start();;
        return agent;
    }

    @Override
    public Resource stop(IRI self) throws Exception {
        I_Agent agent = getAgent(self);
        if (agent==null) return null;
        agent.stop();
        return agent.getStateMachine().getState();
    }
}
