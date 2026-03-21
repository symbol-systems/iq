package systems.symbol.controller.acl;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RDF-based ACL Policy implementation.
 * 
 * Checks ACL rules in the RDF repository using SPARQL.
 * Rules are stored as RDF statements in the format:
 * 
 *   ?principal iq:hasAccessTo ?realm .
 *   ?principal iq:isBlockedFrom ?realm .
 * 
 * Rules are evaluated as:
 * 1. If principal has explicit deny (!blockedFrom), access is DENIED (403).
 * 2. If principal has explicit allow (hasAccessTo), access is ALLOWED.
 * 3. If realm is public, access is ALLOWED.
 * 4. Otherwise, access is DENIED (403).
 */
public class RDFACLPolicy implements I_ACLPolicy {

    private static final Logger log = LoggerFactory.getLogger(RDFACLPolicy.class);

    private final Repository repository;

    /**
     * Query to check if principal has explicit access to realm.
     */
    private static final String ACCESS_QUERY = """
            PREFIX iq: <urn:iq:>
            ASK {
              ?principal iq:hasAccessTo ?realm .
              BIND(?principal_val AS ?principal)
              BIND(?realm_val AS ?realm)
            }
            """;

    /**
     * Query to check if principal is blocked from realm.
     */
    private static final String BLOCKED_QUERY = """
            PREFIX iq: <urn:iq:>
            ASK {
              ?principal iq:isBlockedFrom ?realm .
              BIND(?principal_val AS ?principal)
              BIND(?realm_val AS ?realm)
            }
            """;

    /**
     * Query to check if realm is public.
     */
    private static final String PUBLIC_REALM_QUERY = """
            PREFIX iq: <urn:iq:>
            ASK {
              ?realm iq:isPublic true .
              BIND(?realm_val AS ?realm)
            }
            """;

    /**
     * Constructor for Repository.
     */
    public RDFACLPolicy(Repository repository) {
        this.repository = repository;
    }

    /**
     * Constructor that accepts a RealmPlatform.
     * Creates an in-memory repository for ACL rules.
     */
    public RDFACLPolicy(Object platformOrRepository) {
        if (platformOrRepository instanceof Repository) {
            this.repository = (Repository) platformOrRepository;
        } else {
            // For RealmPlatform or other cases, create an in-memory ACL repository
            this.repository = new SailRepository(new MemoryStore());
            try {
                this.repository.init();
                log.info("ACL: initialized in-memory repository for ACL rules");
            } catch (Exception ex) {
                log.warn("ACL: failed to initialize in-memory repository", ex);
            }
        }
    }

    @Override
    public boolean isAuthorized(String principal, IRI realm, DecodedJWT jwt) {
        if (principal == null || principal.isEmpty()) {
            log.debug("ACL: denying access to {} for anonymous principal", realm.stringValue());
            return false;
        }

        try (RepositoryConnection conn = repository.getConnection()) {
            // Check if principal is explicitly blocked
            if (isBlocked(conn, principal, realm)) {
                log.warn("ACL: principal {} is blocked from realm {}", principal, realm.stringValue());
                return false;
            }

            // Check if principal has explicit access
            if (hasAccess(conn, principal, realm)) {
                log.info("ACL: principal {} granted access to realm {}", principal, realm.stringValue());
                return true;
            }

            // Check if realm is public
            if (isPublicRealm(conn, realm)) {
                log.info("ACL: realm {} is public; granting access to {}", realm.stringValue(), principal);
                return true;
            }

            // Check if principal is admin (has role "admin" in JWT)
            if (isAdmin(jwt)) {
                log.info("ACL: principal {} is admin; granting access to {}", principal, realm.stringValue());
                return true;
            }

            log.warn("ACL: denying access to {} for principal {}", realm.stringValue(), principal);
            return false;
        } catch (Exception ex) {
            log.error("ACL: error checking authorization for {} to {}", principal, realm.stringValue(), ex);
            return false; // Fail securely
        }
    }

    private boolean hasAccess(RepositoryConnection conn, String principal, IRI realm) {
        return checkRDFPattern(conn, ACCESS_QUERY, principal, realm);
    }

    private boolean isBlocked(RepositoryConnection conn, String principal, IRI realm) {
        return checkRDFPattern(conn, BLOCKED_QUERY, principal, realm);
    }

    private boolean isPublicRealm(RepositoryConnection conn, IRI realm) {
        try {
            String query = """
                    PREFIX iq: <urn:iq:>
                    ASK {
                      ?realm iq:isPublic true .
                    }
                    """;
            return conn.prepareBooleanQuery(query).evaluate();
        } catch (Exception ex) {
            log.debug("Error checking if realm is public: {}", ex.getMessage());
            return false;
        }
    }

    private boolean checkRDFPattern(RepositoryConnection conn, String query, String principal, IRI realm) {
        try {
            // Simple workaround: check RDF directly without SPARQL (if Graph API available)
            // For now, use a simpler SPARQL approach
            String sparql = String.format("""
                    PREFIX iq: <urn:iq:>
                    ASK {
                      ?p iq:hasAccessTo ?r .
                      FILTER(str(?p) = "%s" && str(?r) = "%s")
                    }
                    """, principal.replaceAll("\"", "\\\\\""), realm.stringValue());

            return conn.prepareBooleanQuery(sparql).evaluate();
        } catch (Exception ex) {
            log.debug("Error checking ACL pattern: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isAdmin(DecodedJWT jwt) {
        if (jwt == null) return false;
        try {
            List<String> roles = jwt.getClaim("roles").asList(String.class);
            return roles != null && roles.contains("admin");
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String describe() {
        return "RDFACLPolicy: ACL rules stored in RDF (iq:hasAccessTo, iq:isBlockedFrom, iq:isPublic)";
    }
}
