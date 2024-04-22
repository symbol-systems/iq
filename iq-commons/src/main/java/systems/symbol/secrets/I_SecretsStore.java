package systems.symbol.secrets;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.platform.I_LoadSave;

public interface I_SecretsStore {

public I_Secrets getSecrets(IRI agent) throws SecretsException;

public I_Secrets setSecrets(IRI agent, String key, String value);
public I_Secrets setSecrets(IRI agent, I_Secrets secrets);

}
