package systems.symbol.controller.trust;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenAPITest {

    private static final String SECRET = "test-secret";

    @Test
    void testEntitledWhenSubjectMatchesAgent() {
        TokenAPI api = new TokenAPI();
        String token = JWT.create()
                .withSubject("urn:test:agent")
                .withAudience("urn:test:agent")
                .sign(Algorithm.HMAC256(SECRET));

        DecodedJWT jwt = JWT.decode(token);
        IRI agent = Values.iri("urn:test:agent");

        assertTrue(api.entitled(jwt, agent));
    }

    @Test
    void testEntitledWhenAudienceContainsAgent() {
        TokenAPI api = new TokenAPI();
        String token = JWT.create()
                .withSubject("urn:test:different")
                .withAudience("urn:test:agent")
                .sign(Algorithm.HMAC256(SECRET));

        DecodedJWT jwt = JWT.decode(token);
        IRI agent = Values.iri("urn:test:agent");

        assertTrue(api.entitled(jwt, agent));
    }

    @Test
    void testEntitledWhenRoleAdmin() {
        TokenAPI api = new TokenAPI();
        String token = JWT.create()
                .withSubject("urn:test:different")
                .withAudience("urn:test:other")
                .withArrayClaim("roles", new String[]{"user", "admin"})
                .sign(Algorithm.HMAC256(SECRET));

        DecodedJWT jwt = JWT.decode(token);
        IRI agent = Values.iri("urn:test:agent");

        assertTrue(api.entitled(jwt, agent));
    }

    @Test
    void testNotEntitledWhenNoMatch() {
        TokenAPI api = new TokenAPI();
        String token = JWT.create()
                .withSubject("urn:test:different")
                .withAudience("urn:test:other")
                .withArrayClaim("roles", new String[]{"user"})
                .sign(Algorithm.HMAC256(SECRET));

        DecodedJWT jwt = JWT.decode(token);
        IRI agent = Values.iri("urn:test:agent");

        assertFalse(api.entitled(jwt, agent));
    }
}
