package systems.symbol.intent;

import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class IQIntent implements I_Intent {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected IQ iq;
    protected ScriptCatalog library;
    protected Model model;
    static protected SimpleValueFactory vf = SimpleValueFactory.getInstance();
    protected IRI self;
    protected IRI SELF = vf.createIRI("urn:"+getClass().getCanonicalName());
    protected IQIntent() {}

    public void init(IQ iq, Model model, IRI self) throws IOException {
        this.iq = iq;
        library = new ScriptCatalog(iq);
        this.model = model == null ? new DynamicModelFactory().createEmptyModel(): model;
        this.self = self;
    }

    public Model getModel() {
        return model;
    }

    public List<Map<String, Object>> executeQuery(IRI query) {
        SPARQLMapper models = new SPARQLMapper(iq);
        return models.models(query);
    }

    @Override
    abstract public Set<IRI> execute(IRI actor, Resource state, Bindings bindings);

    public IRI getSelf() {
        return this.self;
    }

    public String toString() {
        return this.self.stringValue()+"@"+ SELF;
    }
}
