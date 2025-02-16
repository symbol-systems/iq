package systems.symbol.trust.generate;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;

import systems.symbol.tools.TrustedAPIs;
import systems.symbol.trust.SimpleKeyStore;
import java.security.*;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JWTGen {
protected static final Logger log = LoggerFactory.getLogger(JWTGen.class);

public JWTCreator.Builder generate(String issuer, String subject, String[] audience, int longevitySeconds) {
Date issuedAt = new Date();
Date expiresAt = new Date(issuedAt.getTime() + (longevitySeconds * 1000L));

log.info("trust.jwt: {} -> {} @ {}", issuer, subject, expiresAt);
return JWT.create()
.withIssuer(issuer)
.withSubject(subject)
.withAudience(audience)
.withIssuedAt(issuedAt)
.withNotBefore(issuedAt)
.withExpiresAt(expiresAt);
}

public JWTCreator.Builder generate(String issuer, String subject, String[] audience, int longevitySeconds,
String fullName, String[] roles) {
JWTCreator.Builder builder = generate(issuer, subject, audience, longevitySeconds);
builder.withClaim("fullName", fullName);
builder.withArrayClaim("roles", roles == null ? new String[] {} : roles);
return builder;
}

public String sign(JWTCreator.Builder jwtBuilder, KeyPair keyPair) {
return jwtBuilder.sign(SimpleKeyStore.rsa256(keyPair));
}

public DecodedJWT verify(KeyPair keyPair, String token) {
JWTVerifier verifier = JWT.require(SimpleKeyStore.rsa256(keyPair)).build();
return verifier.verify(token);
}

public static String toBase64(Key key) throws Exception {
return java.util.Base64.getEncoder().encodeToString(key.getEncoded());
}

}
