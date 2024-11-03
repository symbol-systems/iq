package systems.symbol.io;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * symbol.systems (c) 2013-2021
 * Module: systems.symbol.util.io
 * 
 * @author Symbol Systems
 * Date : 10/01/2014
 * Time : 2:54 PM
 */
public class IOStreamCrypto {
byte[] bytePassword = null;
final byte[] ivBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c,
0x0d, 0x0e, 0x0f }; // example
String cipherTransformation = "AES/CFB8/NoPadding";
String cipherSpec = "AES";

public IOStreamCrypto(String password) {
this.bytePassword = password.getBytes();
}

public IOStreamCrypto(byte[] bytePassword) {
this.bytePassword = bytePassword;
}

public CipherOutputStream encrypt(OutputStream out) throws NoSuchPaddingException, NoSuchAlgorithmException,
InvalidAlgorithmParameterException, InvalidKeyException {
final SecretKey key = new SecretKeySpec(bytePassword, cipherSpec);
final IvParameterSpec IV = new IvParameterSpec(ivBytes);
final Cipher cipher = Cipher.getInstance(cipherTransformation);
cipher.init(Cipher.ENCRYPT_MODE, key, IV);
return new CipherOutputStream(new Base64OutputStream(out), cipher);
}

public CipherInputStream decrypt(InputStream in) throws NoSuchPaddingException, NoSuchAlgorithmException,
InvalidAlgorithmParameterException, InvalidKeyException {
final SecretKey key = new SecretKeySpec(bytePassword, cipherSpec);
final IvParameterSpec IV = new IvParameterSpec(ivBytes);
final Cipher cipher = Cipher.getInstance(cipherTransformation);
cipher.init(Cipher.DECRYPT_MODE, key, IV);
return new CipherInputStream(new Base64InputStream(in), cipher);
}
}
