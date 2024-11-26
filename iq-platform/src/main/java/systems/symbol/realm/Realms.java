package systems.symbol.realm;

import com.auth0.jwt.JWTCreator;

import org.eclipse.rdf4j.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.Facts;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.secrets.SecretsException;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.generate.JWTGen;
import systems.symbol.util.IdentityHelper;

public class Realms {
    private static final Logger log = LoggerFactory.getLogger(Realms.class);

    public static Iterable<IRI> trusts(Model model, IRI agent) {
        return Facts.find(model, agent, new IRIs(), false, IQ_NS.TRUSTS);
    }

    public static Iterable<IRI> trusts(Model model, IRI focus, IRIs trusts, boolean recurse) {
        return Facts.find(model, focus, trusts, recurse, IQ_NS.TRUSTS);
    }

    public static String tokenize(String issuer, String[] roles, String self, String name, String[] audience,
            I_Keys keys,
            int durationSeconds) throws SecretsException {
        JWTGen jwtGen = new JWTGen();
        JWTCreator.Builder generator = jwtGen.generate(issuer, self, audience, durationSeconds);
        generator.withClaim("name", name);
        generator.withClaim("jti", IdentityHelper.uuid());
        if (roles.length > 0)
            generator.withArrayClaim("roles", roles);
        return jwtGen.sign(generator, keys.keys());
    }
}
