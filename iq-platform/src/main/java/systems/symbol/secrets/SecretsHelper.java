package systems.symbol.secrets;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class SecretsHelper {
public static final String ALGORITHM = "AES";
public static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

public static I_Secrets unlock(InputStream fileInputStream, String password) throws IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fileInputStream));
byte[] encryptedData = (byte[]) ois.readObject();
return decrypt(encryptedData, password);
}

public static void lock(I_Secrets ownerSecrets, OutputStream fileOutputStream, String password) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fileOutputStream));
byte[] encryptedData = encrypt(ownerSecrets, password);
oos.writeObject(encryptedData);
}

protected SecretKey toKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
int iterations = 10000; // Number of iterations, can be adjusted based on security requirements
int keyLength = 256; // Key length in bits

// Creating a key specification for PBKDF2
KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);

// Using a SecretKeyFactory to derive the key material
SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
SecretKey secretKey = keyFactory.generateSecret(keySpec);
return secretKey;
}

public static byte[] encrypt(I_Secrets data, String password) throws NoSuchPaddingException, NoSuchAlgorithmException,
InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
byte[] key = password.getBytes(StandardCharsets.UTF_8);

Cipher cipher = Cipher.getInstance(TRANSFORMATION);
SecretKey secretKey = new SecretKeySpec(key, ALGORITHM);
cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));
byte[] encryptedData = cipher.doFinal(Serialize(data));
return encryptedData;
}

public static I_Secrets decrypt(byte[] encryptedData, String password) throws NoSuchPaddingException, NoSuchAlgorithmException,
InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, NoSuchPaddingException, IllegalBlockSizeException {

byte[] key = password.getBytes(StandardCharsets.UTF_8);
Cipher cipher = Cipher.getInstance(TRANSFORMATION);
SecretKey secretKey = new SecretKeySpec(key, ALGORITHM);
cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));
byte[] decryptedData = cipher.doFinal(encryptedData);
return (I_Secrets) Deserialize(decryptedData);
}

public static byte[] Serialize(I_Secrets obj) throws IOException {
try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
oos.writeObject(obj);
return bos.toByteArray();
}
}

public static Object Deserialize(byte[] data) throws IOException, ClassNotFoundException {
try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
 ObjectInputStream ois = new ObjectInputStream(bis)) {
return ois.readObject();
}
}
}
