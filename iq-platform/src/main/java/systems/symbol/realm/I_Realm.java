package systems.symbol.platform;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import systems.symbol.finder.FactFinder;
import systems.symbol.secrets.I_Secrets;

import java.net.URISyntaxException;

public interface I_WorkSpace extends RepositoryResolver, I_Self {
    Model getModel();
    Repository getRepository(IRI iri);
    FactFinder getFinder(IRI iri);
    FileObject toFile(IRI iri) throws URISyntaxException, FileSystemException;
    I_Secrets getSecrets();
}
