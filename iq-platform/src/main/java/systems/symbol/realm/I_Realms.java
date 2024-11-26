package systems.symbol.realm;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.finder.I_Corpus;
import systems.symbol.secrets.SecretsException;

import java.util.Set;

public interface I_Realms {
    I_Realm getRealm(IRI self) throws SecretsException, PlatformException;

    // I_Realm getRealm(IRI self, Model model) throws SecretsException;

    I_Corpus<IRI> searcher(IRI realm);

    public Set<IRI> getRealms();
}
