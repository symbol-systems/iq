package systems.symbol.secrets;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretsHelper {
    static final Logger log = LoggerFactory.getLogger(SecretsHelper.class);
    public static final String ALGORITHM = "AES";
    public static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    public static final String KEY_MATTER_ALGORITHM = "PBKDF2WithHmacSHA256";
    protected static final int TIME_STEP = 180;
    protected static final int iterations = 10000;
    protected static final int keyLength = 256;

    public static I_Secrets unlock(InputStream fileInputStream, String password)
            throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException,
            InvalidKeySpecException {
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fileInputStream));
        byte[] encryptedData = (byte[]) ois.readObject();
        return decrypt(encryptedData, password);
    }

    public static void lock(I_Secrets ownerSecrets, OutputStream fileOutputStream, String password)
            throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException,
            NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fileOutputStream));
        byte[] encryptedData = encrypt(ownerSecrets, password);
        oos.writeObject(encryptedData);
    }

    protected SecretKey toKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_MATTER_ALGORITHM);
        return keyFactory.generateSecret(keySpec);
    }

    public static byte[] encrypt(I_Secrets data, String password)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            IOException, InvalidKeySpecException {

        // Generate salt and key from password
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        SecretKey secretKey = new SecretsHelper().toKey(password, salt);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        // Generate a random IV
        byte[] iv = new byte[cipher.getBlockSize()];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        // Serialize the data and encrypt it
        byte[] encryptedData = cipher.doFinal(serialize(data));

        // Combine salt, IV, and encrypted data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(salt);
        outputStream.write(iv);
        outputStream.write(encryptedData);

        return outputStream.toByteArray();
    }

    public static I_Secrets decrypt(byte[] encryptedData, String password)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException,
            IOException, ClassNotFoundException, NoSuchPaddingException, IllegalBlockSizeException,
            InvalidKeySpecException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedData);
        return decrypt(inputStream, password);
    }

    public static I_Secrets decrypt(ByteArrayInputStream inputStream, String password)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException,
            ClassNotFoundException {

        // Extract salt
        byte[] salt = new byte[16];
        inputStream.read(salt);

        // Derive key from password and salt
        SecretKey secretKey = new SecretsHelper().toKey(password, salt);

        // Extract IV
        byte[] iv = new byte[16]; // AES block size
        inputStream.read(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Read encrypted data
        byte[] cipherText = inputStream.readAllBytes();

        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] decryptedData = cipher.doFinal(cipherText);

        return (I_Secrets) deserialize(decryptedData);
    }

    public static byte[] serialize(I_Secrets obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }

    public static String totp(String seed, int length) {
        return totp(seed, length, "HmacSHA1", TIME_STEP);
    }

    public static String totp(String seed) {
        return totp(seed, 6, "HmacSHA1", TIME_STEP).toUpperCase();
    }

    public static String totp(String seed, int length, String algo, int timeSeconds) {
        try {
            long timeCounter = System.currentTimeMillis() / 1000 / timeSeconds;
            byte[] timeBytes = longToBytes(timeCounter);

            byte[] keyBytes = seed.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, algo);
            Mac mac = Mac.getInstance(algo);
            mac.init(signingKey);

            byte[] hash = mac.doFinal(timeBytes);
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24) |
                    ((hash[offset + 1] & 0xFF) << 16) |
                    ((hash[offset + 2] & 0xFF) << 8) |
                    (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, length);

            String code = String.format("%0" + length + "d", otp);
            log.info("totp.code: {} @ {}", code, timeCounter);
            return code;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] longToBytes(long value) {
        return new byte[] {
                (byte) (value >> 56),
                (byte) (value >> 48),
                (byte) (value >> 40),
                (byte) (value >> 32),
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }
}
