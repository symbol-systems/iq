package systems.symbol.controller.acl;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.IRI;

/**
 * Access Control List (ACL) Policy interface.
 * Defines rules for determining whether a principal has access to a realm.
 */
public interface I_ACLPolicy {

/**
 * Checks if the principal (from JWT subject) is authorized to access the given realm.
 *
 * @param principal   The principal from the JWT subject claim (e.g., "user@example.com")
 * @param realm   The realm IRI being accessed (e.g., "urn:iq:realm:public")
 * @param jwt The decoded JWT token with additional claims
 * @return true if principal is authorized for the realm, false otherwise
 */
boolean isAuthorized(String principal, IRI realm, DecodedJWT jwt);

/**
 * Returns a description of this ACL policy for logging/debugging.
 */
String describe();
}
