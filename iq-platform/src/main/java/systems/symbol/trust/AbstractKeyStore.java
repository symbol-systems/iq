package systems.symbol.trust;

import com.auth0.jwt.algorithms.Algorithm;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import systems.symbol.secrets.SecretsException;

import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public abstract class AbstractKeyStore implements I_KeyStore {
    static String algo = "RSA", PRIVATE_KEY_FILENAME = "private.key", PUBLIC_KEY_FILENAME = "public.key";
    static String PRIVATE_KEY = "PRIVATE KEY", PUBLIC_KEY = "PUBLIC KEY";
    int KEY_SIZE = 4096;

    protected AbstractKeyStore() {
    }

    @Override
    public KeyPair keys() throws SecretsException {
        try {
            return load();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SecretsException(e.getMessage());
        }
    }

    public KeyPair newKeys(String algo) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algo);
        keyPairGenerator.initialize(KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    public abstract boolean isProvisioned();

    // Save KeyPair to a folder
    public abstract void save(KeyPair keyPair) throws Exception;

    // Load KeyPair from ByteArrayInputStreams
    public abstract KeyPair load() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException;

    public static Algorithm rsa256(KeyPair keyPair) {
        return Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
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
    public static Key fromPKCS8(String key) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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
                throw new NoSuchAlgorithmException(pemData.getType());
                // }
            }
        }
    }

    // Load KeyPair from streams
    protected KeyPair load(InputStream privateIn, InputStream publicIn)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] privateKeyBytes = privateIn.readAllBytes();
        byte[] publicKeyBytes = publicIn.readAllBytes();

        String privateKeyString = new String(privateKeyBytes);
        String publicKeyString = new String(publicKeyBytes);

        PrivateKey privateKey = (PrivateKey) fromPKCS8(privateKeyString);
        PublicKey publicKey = (PublicKey) fromPKCS8(publicKeyString);

        return new KeyPair(publicKey, privateKey);
    }
}
