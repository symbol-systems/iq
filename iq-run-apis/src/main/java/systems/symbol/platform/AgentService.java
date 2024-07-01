package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.*;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import javax.script.Bindings;

public class AgentService {
    ExecutiveIntent intent;
    I_Agent agent;
    IRI self;
    Model model;
    Bindings state;
    I_Contents scripts;

    public AgentService(IRI self, RepositoryConnection connection, I_Secrets secrets, Bindings state) throws StateException, SecretsException {
        this.self = self;
        this.model = new LiveModel(connection);
        this.scripts = new IQScriptCatalog(this.self, connection);
        this.intent = new ExecutiveIntent(self, model, model,new JSR233(self, model, model, secrets, scripts));
        this.intent.add(new Select(self, connection));
        this.intent.add(new Update(self, connection));
        this.intent.add(new Remodel(self, this.model, scripts));
        this.intent.add(new Construct(self, connection));
        this.state = state;
        this.agent = new ExecutiveAgent(this.self, this.model, intent, null, state);
    }

    public Resource next(Resource state) throws StateException {
        return agent.getStateMachine().transition(state);
    }

    public Bindings getBindings() {
        return state;
    }

    public Model getModel() {
        return model;
    }

    public I_Agent getAgent() {
        return agent;
    }

}
