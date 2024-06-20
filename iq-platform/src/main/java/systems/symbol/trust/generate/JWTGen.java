package systems.symbol.trust.generate;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.jetbrains.annotations.NotNull;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.SimpleKeyStore;

import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;


public class JWTGen  {
    public JWTCreator.Builder generate(@NotNull String issuer, @NotNull String subject, @NotNull String[] audience, int longevitySeconds) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + (longevitySeconds * 1000L));

        return JWT.create()
                .withIssuer(issuer)
                .withSubject(subject)
                .withAudience(audience)
                .withIssuedAt(issuedAt)
                .withNotBefore(issuedAt)
                .withExpiresAt(expiresAt);
    }

    public JWTCreator.Builder generate(@NotNull String issuer, @NotNull String subject, @NotNull String[] audience, int longevitySeconds, @NotNull String fullName, String[] roles) {
        JWTCreator.Builder builder = generate(issuer, subject, audience, longevitySeconds);
        builder.withClaim("fullName", fullName);
        builder.withArrayClaim("roles", roles == null ? new String[]{} : roles);
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
