package systems.symbol.controller.platform;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.controller.acl.I_ACLPolicy;
import systems.symbol.controller.acl.RDFACLPolicy;
import systems.symbol.controller.responses.OopsException;

public class GuardedAPI extends RealmAPI {

    private I_ACLPolicy aclPolicy;

    protected I_ACLPolicy getACLPolicy() {
        if (aclPolicy == null && platform != null && platform.getRealms() != null) {
            // Lazy init: create RDF-based ACL policy
            try {
                aclPolicy = new RDFACLPolicy(platform);
            } catch (Exception ex) {
                log.warn("Failed to initialize ACL policy: {}", ex.getMessage());
            }
        }
        return aclPolicy;
    }

    @Override
    public boolean entitled(DecodedJWT jwt, IRI agent) {
        return jwt.getAudience().contains(agent.stringValue());
    }

    /**
     * Checks ACL enforcement for realm access.
     * Enforces: (principal, realm) ACL rules.
     *
     * @param principal The principal (JWT subject)
     * @param realm     The realm being accessed
     * @param jwt       The decoded JWT token
     * @throws OopsException if principal is not authorized for realm
     */
    public void checkRealmAccess(String principal, IRI realm, DecodedJWT jwt) throws OopsException {
        I_ACLPolicy policy = getACLPolicy();
        if (policy != null) {
            boolean authorized = policy.isAuthorized(principal, realm, jwt);
            if (!authorized) {
                log.warn("ACL: principal {} denied access to realm {}", principal, realm.stringValue());
                throw new OopsException("ux.realm.unauthorized", Response.Status.FORBIDDEN);
            }
            log.info("ACL: principal {} authorized for realm {} (policy: {})", 
                principal, realm.stringValue(), policy.describe());
        }
    }
}
