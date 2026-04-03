package systems.symbol.realm;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import systems.symbol.platform.I_Self;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.trust.I_Keys;

import java.net.URISyntaxException;
import java.security.KeyPair;

public interface I_Realm extends I_Self, I_Keys { 
Model getModel();

Repository getRepository();

FileObject locate(IRI iri) throws URISyntaxException, FileSystemException;

I_Secrets getSecrets();

KeyPair keys();

}
