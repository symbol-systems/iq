package systems.symbol.trust;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * A static helper with lots of useful tools
 */
public class Locksmith {
    static String hexStr = "0123456789ABCDEF";
    static SecureRandom secureSeed = new SecureRandom();

    /*
     * Convert into hex values
     */
    private static String hex(byte [] p) {
        String newStr = new String();
        // byte [] p = binStr.getBytes();
        for(int k=0; k < p.length; k++ ){
            int j = ( p[k] >> 4 )&0xF;
            newStr = newStr + hexStr.charAt( j );
            j = p[k]&0xF;
            newStr = newStr + hexStr.charAt( j ) + " ";
        }
        return newStr;
    }

    /*
     * Encrypt a message using a certificate file cacert.pem (contains public key).
     * Decrypt the encrypted message using a private key file (cakey.p8c).
     */
    // Obtain a RSA Cipher Object
    public static Cipher cipher() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        return Cipher.getInstance("RSA/ECB/PKCS1Padding","BC");
    }

    public static X509Certificate toCertificate(InputStream certStream) throws CertificateException {
        // Loading certificate file
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);
        return cert;
    }

    public static RSAPublicKey loadPublicKeyFromCertificate(InputStream certStream) throws CertificateException {
        X509Certificate cert = Locksmith.toCertificate(certStream);

        // Read the public key from certificate file
        RSAPublicKey certPublicKey = (RSAPublicKey) cert.getPublicKey();
        return certPublicKey;
//        byte[] tempPub = certPublicKey.getEncoded();
//        String sPub = new String(tempPub);
//        System.out.println("Public key from certificate file:\n" + hex(sPub) + "\n");
//        System.out.println("Public Key Algorithm = " + cert.getPublicKey().getAlgorithm() + "\n" );
//        return certPublicKey;
    }

    public static byte[] encrypt(InputStream certStream, InputStream keyStream, String message) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // Initialize the cipher for encryption
        Cipher cipher = Locksmith.cipher();
        RSAPublicKey publicKey = Locksmith.loadPublicKeyFromCertificate(certStream);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, Locksmith.secureSeed);
        return encrypt(cipher,keyStream, message);
    }

    public static byte[] encrypt(Cipher cipher, InputStream keyStream, String message) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        PrivateKey priv = Locksmith.loadPrivateKey(keyStream);
        System.out.println("Loaded " + priv.getAlgorithm() + " " + priv.getFormat() + " private key.");
        // Set plain message
        byte[] messageBytes = message.getBytes();
        System.out.println("Plain message:\n" + message + "\n" );

        // Encrypt the message
        byte[] ciphertextBytes = cipher.doFinal(messageBytes);
        System.out.println("Message encrypted with certificate file public key:\n" + new String(ciphertextBytes) + "\n");
        return ciphertextBytes;
    }

    public static  PrivateKey loadPrivateKey(InputStream keyStream) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Loading private key file
        byte[] encKey=new byte[keyStream.available()];
        keyStream.read(encKey);

        // Read the private key from file
        System.out.println("RSA PrivateKeyInfo: " + encKey.length + " bytes\n") ;
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(encKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        System.out.println("KeyFactory Object Info:");
        System.out.println("Algorithm = "+keyFactory.getAlgorithm());
        System.out.println("Provider = "+keyFactory.getProvider());
        PrivateKey priv= (RSAPrivateKey) keyFactory.generatePrivate(privKeySpec);
        return priv;

    }

    public static  byte[] decrypt(InputStream keyStream, byte[] ciphertextBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Locksmith.cipher();
        PrivateKey privateKey = Locksmith.loadPrivateKey(keyStream);
        // Initialize the cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, privateKey, secureSeed);

        // Decrypt the message
        byte[] textBytes = cipher.doFinal(ciphertextBytes);
        System.out.println("Message decrypted with file private key:\n" + new String(textBytes) + "\n");
        return textBytes;
    }

    public static  void check(InputStream keyStream, InputStream certStream, String message) throws NoSuchPaddingException, IllegalBlockSizeException, CertificateException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, BadPaddingException, NoSuchProviderException, InvalidKeyException {
        byte[] encrypted = Locksmith.encrypt(certStream, keyStream, message);
        System.out.println("check.encrypted:" + new String(encrypted) + "\n");
        byte[] decrypted = Locksmith.decrypt(certStream, encrypted);
        System.out.println("check.decrypted:" + new String(decrypted) + "\n");
    }

    public static  KeyStore createEmptyKeyStore() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null,null);
        return keyStore;
    }

    public static  X509Certificate loadCertificate(InputStream publicCertIn) throws IOException, GeneralSecurityException {

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        X509Certificate cert = (X509Certificate)factory.generateCertificate(publicCertIn);

        return cert;

    }
//    public PrivateKey loadPrivateKey(InputStream privateKeyIn) throws IOException, GeneralSecurityException {
//
//        //need the full file - org.apache.commons.io.IOUtils is handy
//        byte[] fullFileAsBytes = IOUtils.toByteArray( privateKeyIn );
//        //remember this is supposed to be a text source with the BEGIN/END and base64 in the middle of the file
//        String fullFileAsString = new String(fullFileAsBytes);
//
//        //nifty regular expression to extract out between BEGIN/END
//        Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
//        String encoded = parse.matcher(fullFileAsString).replaceFirst("$1");
//
//        //decode the Base64 string
//        byte[] keyDecoded = Base64.getMimeDecoder().decode(encoded);
//
//        //for my example, the source is in common PKCS#8 format
//        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyDecoded);
//
//        //from there we can use the KeyFactor to generate
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
//
//        return privateKey;
//    }

    public static  KeyStore createKeyStore(InputStream publicCertIn, InputStream privateKeyIn) throws IOException, GeneralSecurityException {

        KeyStore keyStore = createEmptyKeyStore();

        X509Certificate publicCert = loadCertificate(publicCertIn);

        PrivateKey privateKey = loadPrivateKey(privateKeyIn);

        keyStore.setCertificateEntry("aliasForCertHere", publicCert);

        keyStore.setKeyEntry("aliasForPrivateKeyHere", privateKey, "PasswordForPrivateKeyHere".toCharArray(), new X509Certificate[]{publicCert});

        return keyStore;
    }

    public static  byte[] convertKeyStoreToBytes(KeyStore keyStore) throws IOException, GeneralSecurityException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, "PasswordForPrivateKeyHere".toCharArray());
        byte[] bytes = out.toByteArray();
        return bytes;
    }
    public static  void loadKeys() {

    }

    public static  void saveKeys() {

    }
}