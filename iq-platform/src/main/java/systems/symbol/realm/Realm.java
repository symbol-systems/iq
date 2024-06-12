package systems.symbol.realm;

import org.apache.commons.vfs2.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import systems.symbol.finder.FactFinder;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.secrets.I_Secrets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class SelfSpace implements I_Realm {
    FileSystemManager vfs;
    private final Model model;
    private final Repository  repository;
    private final FactFinder finder;
    private final I_Secrets secrets;
    private final IRI self;
    private final Map<IRI, I_LLMConfig> llmConfig = new HashMap<>();

    public SelfSpace(IRI self, Model model, Repository repository, FactFinder finder, I_Secrets secrets, FileSystemManager vfs)  {
        this.self = self;
        this.model = model;
        this.repository = repository;
        this.finder = finder;
        this.secrets = secrets;
        this.vfs = vfs;
    }

    @Override
    public Model getModel() {
        return this.model;
    }

    @Override
    public Repository getRepository() {
        return this.repository;
    }

    @Override
    public FactFinder getFinder() {
        return this.finder;
    }

    @Override
    public FileObject toFile(IRI iri) throws URISyntaxException, FileSystemException {
        return vfs.resolveFile( new URI(iri.stringValue()));
    }

    @Override
    public I_Secrets getSecrets() {
        return this.secrets;
    }

    @Override
    public IRI getSelf() {
        return self;
    }

    public String toString() {
        return getClass().getName()+"["+self.stringValue()+"]";
    }
}
