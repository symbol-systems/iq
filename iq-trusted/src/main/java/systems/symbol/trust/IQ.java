package systems.symbol.trust;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.I_Agent;
import systems.symbol.intent.JSR233;
import systems.symbol.platform.I_Self;

public class IQ extends JSR233 {
    private final I_Agent agent;
    private final Repository repository;

    public IQ(I_Agent agent, Model model, Repository repository) {
        super(agent, model);
        this.agent = agent;
        this.repository = repository;
    }

    public IQ(I_Agent agent, Model model) {
        this(agent, model, null);
    }

    public IRI toIRI(String relativePath) {
        IRI self = agent.getSelf();
        return Values.iri(self.stringValue(), relativePath);
    }

    public RepositoryConnection getConnection() {
        if (repository == null) {
            throw new IllegalStateException("Repository is not configured");
        }
        return repository.getConnection();
    }

    public IRI getIdentity() {
        IRI self = agent.getSelf();
        if (self != null) {
            return self;
        }
        if (agent instanceof I_Self) {
            return ((I_Self) agent).getSelf();
        }
        return null;
    }
}
