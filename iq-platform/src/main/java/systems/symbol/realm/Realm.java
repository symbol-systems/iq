package systems.symbol.realm;

import org.apache.commons.vfs2.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import systems.symbol.finder.FactFinder;
import systems.symbol.llm.DefaultLLConfig;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.secrets.I_Secrets;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Realm implements I_Realm {
    FileSystemManager vfs;
    private final Model model;
    private final Repository  repository;
    private final FactFinder finder;
    private final I_Secrets secrets;
    private final IRI self;
    private final Map<String, I_LLMConfig> llm = new HashMap<>();
    private final KeyPair keys;

    public Realm(IRI self, Model model, Repository repository, FactFinder finder, I_Secrets secrets, FileSystemManager vfs, KeyPair keys)  {
        this.self = self;
        this.model = model;
        this.repository = repository;
        this.finder = finder;
        this.secrets = secrets;
        this.vfs = vfs;
        this.keys = keys;
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
    public KeyPair keys() {
        return keys;
    }

    @Override
    public IRI getSelf() {
        return self;
    }


    public String toString() {
        return getClass().getName()+"["+self.stringValue()+"]";
    }
}
