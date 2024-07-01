package systems.symbol.controller.platform;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.IRI;

public class GuardedAPI extends RealmAPI {

    @Override
    public boolean entitled(DecodedJWT jwt, IRI agent) {
        return jwt.getAudience().contains(agent.stringValue());
    }

}
