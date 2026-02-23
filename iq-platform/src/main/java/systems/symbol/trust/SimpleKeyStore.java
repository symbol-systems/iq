package systems.symbol.trust;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class SimpleKeyStore extends AbstractKeyStore {
    File keysHome;

    public SimpleKeyStore(File keysHome) throws Exception {
        this.keysHome = keysHome;
        keysHome.mkdirs();
        if (!isProvisioned()) {
            save(newKeys(algo));
        }
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
}
