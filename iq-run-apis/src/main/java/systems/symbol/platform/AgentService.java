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
import systems.symbol.rdf4j.sparql.ModelScriptCatalog;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.I_Secrets;

import javax.script.Bindings;

public class AgentService {
    ExecutiveIntent intent;
    I_Agent agent;
    IRI self;
    Model model;
    Bindings state;
    I_Contents scripts;

    public AgentService(Platform platform, IRI self, Model model, I_Secrets secrets, Bindings state) throws StateException {
        this.self = self;
        this.model = model;
        this.intent = new ExecutiveIntent(self, model,new JSR233(self, model, secrets));
        this.agent = new ExecutiveAgent(this.self, this.model, intent, null, state);
//        this.intent.add(new Find(self, this.model, platform.getFactFinder()));
        this.state = state;
        this.scripts = new ModelScriptCatalog(this.model);
    }

    public AgentService(IRI self, RepositoryConnection connection, I_Secrets secrets, Bindings state) throws StateException {
        this.self = self;
        this.model = new LiveModel(connection);
        this.intent = new ExecutiveIntent(self, model,new JSR233(self, model, secrets));
        this.intent.add(new Select(self, connection));
        this.intent.add(new Update(self, connection));
        this.intent.add(new Remodel(self, this.model, scripts));
        this.intent.add(new Construct(self, connection));
        this.agent = new ExecutiveAgent(this.self, this.model, intent, null, state);
        this.scripts = new IQScriptCatalog(this.self, connection);
        this.state = state;
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
