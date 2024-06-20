package systems.symbol.trust.generate;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;
import systems.symbol.trust.SimpleKeyStore;

import java.io.File;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JWTGenTest {
    String issuer = "JO";
    String subject = "alice";
    String audience = "test";
    File secretsFolder = new File("tested/tmp/secrets");

    @Test
    void testKeySaveLoad() throws Exception {
        JWTGen jwtGen = new JWTGen();
        SimpleKeyStore keyStore = new SimpleKeyStore(secretsFolder);
        KeyPair keyPair = keyStore.keys();
        keyStore.save(keyPair);
        System.out.println("JWT saved: " + secretsFolder.getAbsolutePath());
        KeyPair keyPairLoaded = keyStore.load();

        assertTrue(keyPair.getPublic().equals(keyPairLoaded.getPublic()));
        assertTrue(keyPair.getPrivate().equals(keyPairLoaded.getPrivate()));
    }

    @Test
    void generateJWT() throws Exception {
        JWTGen jwtGen = new JWTGen();
        SimpleKeyStore keyStore = new SimpleKeyStore(secretsFolder);
        KeyPair keyPair = keyStore.keys();

        JWTCreator.Builder jwtBuilder = jwtGen.generate(issuer, subject, new String[]{audience}, 60);

        jwtBuilder.withClaim("hello", "world");
        String jwt = jwtGen.sign(jwtBuilder, keyPair);
//        System.out.println("Generated JWT: " + jwt);

        DecodedJWT verified = jwtGen.verify(keyPair, jwt);
//        System.out.println("Verified JWT: " + verified.getPayload());
        DecodedJWT decoded = JWT.decode(jwt);
        System.out.println("JWT: " + decoded.getPayload());
        assertEquals(verified.getPayload(), decoded.getPayload());

        System.out.println("JWT subject: " + decoded.getSubject());
        System.out.println("JWT issuer: " + decoded.getIssuer());
        System.out.println("JWT audience: " + decoded.getAudience());
        System.out.println("JWT expires: " + decoded.getExpiresAt());

        assertEquals(decoded.getSubject(), subject);
        for (boolean b : new boolean[]{decoded.getSubject().contains(subject), decoded.getAudience().contains(audience), decoded.getIssuer().equals(issuer)}) {
            assertTrue(b);
        }

    }
}