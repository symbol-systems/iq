package systems.symbol.realm;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import systems.symbol.finder.FactFinder;
import systems.symbol.finder.I_Found;
import systems.symbol.finder.I_Indexer;
import systems.symbol.finder.I_Search;
import systems.symbol.platform.I_Self;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.trust.I_Keys;

import java.net.URISyntaxException;
import java.security.KeyPair;

public interface I_Realm extends I_Self, I_Keys, I_Search<I_Found<IRI>>, I_Indexer {
Model getModel();
Repository getRepository();
FactFinder getFinder();
//I_Search<I_Found<IRI>> getSearch(IRI index);
FileObject toFile(IRI iri) throws URISyntaxException, FileSystemException;
I_Secrets getSecrets();
KeyPair keys();

}
