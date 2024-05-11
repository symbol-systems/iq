package systems.symbol.trust.generate;

import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

public class KeyGen {

private final String keypairAlgorithm;
private final int keySize;

public KeyGen(String keypairAlgorithm, int size) {
this.keypairAlgorithm = keypairAlgorithm;
this.keySize = size;
}

public KeyGen() {
this("EC", 256);
}

public KeyPair keys() throws NoSuchAlgorithmException, CertificateException, CertificateEncodingException, OperatorCreationException, IOException {
SecureRandom random = new SecureRandom();
// create keypair
KeyPairGenerator keypairGen = KeyPairGenerator.getInstance(keypairAlgorithm);
keypairGen.initialize(keySize, random);
return keypairGen.generateKeyPair();
}


}
