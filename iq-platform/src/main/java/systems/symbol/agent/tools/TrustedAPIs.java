package systems.symbol.agent.tools;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.realm.Facts;
import systems.symbol.secrets.APISecrets;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Set;

public class TrustedAPIs {
    protected static final Logger log = LoggerFactory.getLogger(TrustedAPIs.class);

    public static I_Secrets trusted(Model model, IRI agent, I_Secrets _secrets) throws SecretsException {
        APISecrets secrets = new APISecrets(_secrets);
        Optional<IRI> name = Models.getPropertyIRI(model, agent, IQ_NS.SECRET);
        if (!name.isPresent()) {
            log.warn("missing secret name: {}", agent);
            return secrets;
        }
        String key = name.get().getLocalName();
        String secret = _secrets.getSecret(key);
        if (secret == null) throw new SecretsException(name.get().stringValue());

        Set<IRI> iris = Facts.find(model, agent, IQ_NS.TRUSTS);
        for (IRI iri : iris) {
            secrets.grant(iri.stringValue(), key);
        }
        log.info("secret.grant: {} -> {}", key, iris);
        return secrets;
    }

}

