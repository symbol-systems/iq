package systems.symbol.agent.tools;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.platform.IQ_NS;
import systems.symbol.secrets.APISecrets;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import java.util.Optional;

public class TrustedAPIs {
    protected static final Logger log = LoggerFactory.getLogger(TrustedAPIs.class);

    public static I_Secrets trusted(Model model, IRI agent, I_Secrets _secrets) throws SecretsException {
        APISecrets secrets = new APISecrets(_secrets);
        Optional<Literal> name = Models.getPropertyLiteral(model, agent, IQ_NS.NAME);
        if (!name.isPresent()) {
            log.warn("missing secret name: {}", agent);
            return secrets;
        }
        String key = name.get().stringValue();
        String secret = _secrets.getSecret(key);
        if (secret == null) {
            throw new SecretsException("missing secret: "+key);
        }

        Iterable<Statement> trusted = model.getStatements(agent, IQ_NS.TRUSTS, null);
        for (Statement st : trusted) {
            Value url = st.getObject();
            secrets.grant(url.stringValue(), key);
            log.info("secret.grant: {} -> {}", key, url.stringValue());
        }
        return secrets;
    }

}

