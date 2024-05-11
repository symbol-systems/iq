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

import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;


public class JWTGen {
static String algo = "RSA", PRIVATE_KEY_FILENAME = "private.key", PUBLIC_KEY_FILENAME = "public.key";
static String PRIVATE_KEY= "PRIVATE KEY", PUBLIC_KEY = "PUBLIC KEY";
int KEY_SIZE = 4096;

public KeyPair keys() throws NoSuchAlgorithmException {
KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algo);
keyPairGenerator.initialize(KEY_SIZE);
return keyPairGenerator.generateKeyPair();
}

public boolean isProvisioned(File folder) {
File privateKeyFile = new File(folder, PRIVATE_KEY_FILENAME);
File publicKeyFile = new File(folder, PUBLIC_KEY_FILENAME);
return folder.exists() && privateKeyFile.exists() && publicKeyFile.exists();
}

// Save KeyPair to a folder
public void save(File folder, KeyPair keyPair) throws Exception {
folder.mkdirs();
try (FileOutputStream privateOut = new FileOutputStream(new File(folder, PRIVATE_KEY_FILENAME));
 FileOutputStream publicOut = new FileOutputStream(new File(folder, PUBLIC_KEY_FILENAME))) {
privateOut.write(toPKCS8(keyPair.getPrivate()).getBytes());
publicOut.write(toPKCS8(keyPair.getPublic()).getBytes());
}
}

// Load KeyPair from ByteArrayInputStreams
public KeyPair load(File folder) throws Exception {
File privateKeyFile = new File(folder, PRIVATE_KEY_FILENAME);
File publicKeyFile = new File(folder, PUBLIC_KEY_FILENAME);

try (FileInputStream privateIn = new FileInputStream(privateKeyFile);
 FileInputStream publicIn = new FileInputStream(publicKeyFile)) {
byte[] privateKeyBytes = new byte[(int) privateKeyFile.length()];
byte[] publicKeyBytes = new byte[(int) publicKeyFile.length()];
int readPrivate = privateIn.read(privateKeyBytes);
int readPublic = publicIn.read(publicKeyBytes);
//log.r

String privateKeyString = new String(privateKeyBytes);
String publicKeyString = new String(publicKeyBytes);

PrivateKey privateKey = (PrivateKey) fromPKCS8(privateKeyString);
PublicKey publicKey = (PublicKey) fromPKCS8(publicKeyString);

return new KeyPair(publicKey, privateKey);
}
}

public Algorithm rsa256(KeyPair keyPair) {
return Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
}

public JWTCreator.Builder generate(@NotNull String issuer, @NotNull String subject, @NotNull String audience, int longevitySeconds) {
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

public JWTCreator.Builder generate(@NotNull String issuer, @NotNull String subject, @NotNull String audience, int longevitySeconds, @NotNull String fullName, String[] roles) {
JWTCreator.Builder builder = generate(issuer, subject, audience, longevitySeconds);
builder.withClaim("fullName", fullName);
builder.withArrayClaim("roles", roles == null ? new String[]{} : roles);
return builder;
}

//public JWTCreator.Builder claim(JWTCreator.Builder jwtBuilder, String key, String value) {
//jwtBuilder.withClaim(key,value);
//return jwtBuilder;
//}
//
//public JWTCreator.Builder claims(JWTCreator.Builder jwtBuilder, String key, String[] values) {
//jwtBuilder.withArrayClaim(key, values);
//return jwtBuilder;
//}

public String sign(JWTCreator.Builder jwtBuilder, KeyPair keyPair) {
return jwtBuilder.sign(rsa256(keyPair));
}

public DecodedJWT verify(KeyPair keyPair, String token) {
JWTVerifier verifier = JWT.require(rsa256(keyPair)).build();
return verifier.verify(token);
}

public static String toBase64(Key key) throws Exception {
return java.util.Base64.getEncoder().encodeToString(key.getEncoded());
}

// Convert key to PKCS#8 format
public static String toPKCS8(Key key) throws Exception {
StringWriter stringWriter = new StringWriter();
try (PemWriter pemWriter = new PemWriter(stringWriter)) {
String type = (key instanceof PrivateKey) ? PRIVATE_KEY : PUBLIC_KEY;
pemWriter.writeObject(new PemObject(type, key.getEncoded()));
}
return stringWriter.toString();
}

// Convert PKCS#8 formatted key to Key object
public static Key fromPKCS8(String key) throws Exception {
try (PemReader pemReader = new PemReader(new StringReader(key))) {
PemObject pemData = pemReader.readPemObject();
byte[] content = pemData.getContent();
KeyFactory keyFactory = KeyFactory.getInstance(algo);

if (pemData.getType().equals(PRIVATE_KEY)) {
PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(content);
return keyFactory.generatePrivate(pkcs8KeySpec);
} else if (pemData.getType().equals(PUBLIC_KEY)) {
X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(content);
return keyFactory.generatePublic(x509KeySpec);
} else {
throw new IllegalArgumentException("Unsupported key type: " + pemData.getType());
}
}
}
}
