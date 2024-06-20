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

public class SimpleKeyStore implements I_KeyStore {
    static String algo = "RSA", PRIVATE_KEY_FILENAME = "private.key", PUBLIC_KEY_FILENAME = "public.key";
    static String PRIVATE_KEY = "PRIVATE KEY", PUBLIC_KEY = "PUBLIC KEY";
    File keysHome;
    int KEY_SIZE = 4096;

    public SimpleKeyStore(File keysHome) throws Exception {
        this.keysHome = keysHome;
        if (keysHome.mkdirs() && !isProvisioned()) {
            save(newKeys(algo));
        }
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

    public boolean isProvisioned() {
        File privateKeyFile = new File(keysHome, PRIVATE_KEY_FILENAME);
        File publicKeyFile = new File(keysHome, PUBLIC_KEY_FILENAME);
        return keysHome.exists() && privateKeyFile.exists() && publicKeyFile.exists();
    }

    // Save KeyPair to a folder
    public void save(KeyPair keyPair) throws Exception {
        try (FileOutputStream privateOut = new FileOutputStream(new File(keysHome, PRIVATE_KEY_FILENAME));
             FileOutputStream publicOut = new FileOutputStream(new File(keysHome, PUBLIC_KEY_FILENAME))) {
            privateOut.write(toPKCS8(keyPair.getPrivate()).getBytes());
            publicOut.write(toPKCS8(keyPair.getPublic()).getBytes());
        }
    }

    // Load KeyPair from ByteArrayInputStreams
    public KeyPair load() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File privateKeyFile = new File(keysHome, PRIVATE_KEY_FILENAME);
        File publicKeyFile = new File(keysHome, PUBLIC_KEY_FILENAME);

        try (FileInputStream privateIn = new FileInputStream(privateKeyFile);
             FileInputStream publicIn = new FileInputStream(publicKeyFile)) {
            byte[] privateKeyBytes = new byte[(int) privateKeyFile.length()];
            byte[] publicKeyBytes = new byte[(int) publicKeyFile.length()];
            int readPrivate = privateIn.read(privateKeyBytes);
            int readPublic = publicIn.read(publicKeyBytes);
            assert readPublic > 0;
            assert readPrivate > 0;

            String privateKeyString = new String(privateKeyBytes);
            String publicKeyString = new String(publicKeyBytes);

            PrivateKey privateKey = (PrivateKey) fromPKCS8(privateKeyString);
            PublicKey publicKey = (PublicKey) fromPKCS8(publicKeyString);

            return new KeyPair(publicKey, privateKey);
        }
    }

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
//            }
            }
        }
    }
}
