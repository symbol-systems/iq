package systems.symbol.io;

/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * symbol.systems (c) 2013-2021
 * Module: systems.symbol.file
 * 
 * @author Symbol Systems
 *         Date : 24/10/2013
 *         Time : 7:18 PM
 */
public class Fingerprint {
    private final static Logger log = LoggerFactory.getLogger(Fingerprint.class);
    public final static String DEFAULT_ALGO = "SHA-256";

    public static String identify(File file) throws IOException, NoSuchAlgorithmException {
        return identify(Files.newInputStream(file.toPath()), 4096);
    }

    public static String identify(InputStream in) throws IOException, NoSuchAlgorithmException {
        return identify(in, 4096);
    }

    public static String identify(InputStream in, int blockSize) throws IOException, NoSuchAlgorithmException {
        return identify(in, blockSize, DEFAULT_ALGO);
    }

    public static String identify(InputStream in, int blockSize, String algorithm)
            throws IOException, NoSuchAlgorithmException {
        if (in.markSupported())
            in.reset();
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[blockSize];

        while (in.read(buffer) > 0) {
            md.update(buffer);
        }
        in.close();
        return toHex(md.digest());
    }

    public static String identify(byte[] buffer) throws IOException, NoSuchAlgorithmException {
        return identify(buffer, DEFAULT_ALGO);
    }

    public static String identify(byte[] buffer, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(buffer);
        return toHex(md.digest());
    }

    public static String identify(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_ALGO);
            md.update(content.getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static String toMD5(String s) throws NoSuchAlgorithmException {
        return encode(s, null, "MD5");
    }

    public static String encode(String data, String salt, String algo) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);
        md.update(data.getBytes());
        if (salt != null)
            md.update(salt.getBytes());
        return toHex(md.digest());
    }

    public static String copy(InputStream in, OutputStream out, int blockSize)
            throws IOException, NoSuchAlgorithmException {
        return copy(in, out, blockSize, DEFAULT_ALGO);
    }

    public static String copy(InputStream in, OutputStream out, int blockSize, String algorithm)
            throws IOException, NoSuchAlgorithmException {
        if (in.markSupported())
            in.reset();
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[blockSize];

        while (in.read(buffer) > 0) {
            md.update(buffer);
            out.write(buffer);
            out.flush();
        }
        return toHex(md.digest());
    }

    private static String toHex(byte[] data) {
        StringBuilder buffer = new StringBuilder(data.length * 2);
        for (byte b : data) {
            buffer.append(String.format("%02x", b & 0xFF));
        }
        return buffer.toString();
    }
}
