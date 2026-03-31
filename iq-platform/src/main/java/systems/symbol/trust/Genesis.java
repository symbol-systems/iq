package systems.symbol.trust;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * Bootstrap a Trust Chain
 *
 * create the Java Keystore as the wallet
 *
 * create the sovereign key
 * create 5 guardians keys
 * two are cold / dormant
 * three are designated custodians - majeur, disaster and governance.
 * Governance is used through the trust chain operations
 * each custodian is authorized as an intermediate certified authority
 *
 */

public class Genesis {
    private static final Logger log = LoggerFactory.getLogger(Genesis.class);

    /**
     * Bootstrap a new trust chain from certificate and key streams.
     *
     * @param certStream X509 certificate input stream
     * @param keyStream  PKCS8 private key input stream
     * @param password   keystore password
     * @return the initialized KeyStore
     * @throws IOException              if I/O fails
     * @throws GeneralSecurityException if crypto operation fails
     */
    public static KeyStore bootstrap(InputStream certStream, InputStream keyStream, char[] password)
            throws IOException, GeneralSecurityException {
        log.info("genesis.bootstrap: initializing trust chain");
        KeyStore keyStore = Locksmith.createKeyStore(certStream, keyStream, password);
        log.info("genesis.bootstrap: trust chain created, {} entries", keyStore.size());
        return keyStore;
    }
}
