package systems.symbol.trust.generate;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JWTGenTest {
String issuer = "JO";
String subject = "alice";
String audience = "test";
File secretsFolder = new File("tested/tmp/secrets");

@Test
void testKeySaveLoad() throws Exception {
JWTGen jwtGen = new JWTGen();
KeyPair keyPair = jwtGen.keys();
jwtGen.save(secretsFolder, keyPair);
System.out.println("JWT saved: " + secretsFolder.getAbsolutePath());
KeyPair keyPairLoaded = jwtGen.load(secretsFolder);

assertTrue(keyPair.getPublic().equals(keyPairLoaded.getPublic()));
assertTrue(keyPair.getPrivate().equals(keyPairLoaded.getPrivate()));
}

@Test
void generateJWT() throws Exception {
JWTGen jwtGen = new JWTGen();
KeyPair keyPair = jwtGen.keys();
System.out.println("privateKey: " + JWTGen.toPKCS8(keyPair.getPrivate()));
System.out.println("publicKey: " + JWTGen.toPKCS8(keyPair.getPublic()) );

JWTCreator.Builder jwtBuilder = jwtGen.generate(issuer, subject, audience, 60);

jwtGen.claim(jwtBuilder, "hello", "world");
String jwt = jwtGen.sign(jwtBuilder, keyPair);
System.out.println("Generated JWT: " + jwt);

DecodedJWT verified = jwtGen.verify(keyPair, jwt);
System.out.println("Verified JWT: " + verified.getPayload());
DecodedJWT decoded = JWT.decode(jwt);
System.out.println("Decoded JWT: " + decoded.getPayload());
assertTrue( verified.getPayload().equals(decoded.getPayload()));

System.out.println("JWT subject: " + decoded.getSubject());
System.out.println("JWT issuer: " + decoded.getIssuer());
System.out.println("JWT audience: " + decoded.getAudience());
System.out.println("JWT expires: " + decoded.getExpiresAt());

assertTrue( decoded.getSubject().equals(subject));
assertTrue( decoded.getAudience().contains(audience));
assertTrue( decoded.getIssuer().equals(issuer));

}
}