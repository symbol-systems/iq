package systems.symbol.io;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * systems.symbol (c) 2013-2021
 * Module: systems.symbol.file
 * @author Symbol Systems
 * Date  : 24/10/2013
 * Time  : 7:18 PM
 */
public class Fingerprint {
    private final static Logger log = LoggerFactory.getLogger( Fingerprint.class );
    public final static String DEFAULT_ALGO = "SHA-1";

    public static String identify(File file) throws IOException, NoSuchAlgorithmException {
        return identify(new FileInputStream(file), 4096);
    }

    public static String identify(InputStream in) throws IOException, NoSuchAlgorithmException {
        return identify(in, 4096);
    }

    public static String identify(InputStream in, int blocksize) throws IOException, NoSuchAlgorithmException {
        return identify(in, blocksize, DEFAULT_ALGO);
    }

    public static String identify(InputStream in, int blocksize, String algorithm) throws IOException, NoSuchAlgorithmException {
        if (in.markSupported()) in.reset();
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[blocksize];

        while( in.read(buffer)>0 ) {
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

    public static String identify(String buffer) {
        try {
            MessageDigest md = MessageDigest.getInstance(DEFAULT_ALGO);
            md.update(buffer.getBytes(StandardCharsets.UTF_8));
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(),e);
            return null;
        }
    }

    public static String toMD5(String s) throws NoSuchAlgorithmException {
        return encode(s, null, "MD5");
    }

    public static String toSHA256(String s) throws NoSuchAlgorithmException {
        return encode(s, null, "SHA-256");
    }

    public static String toSHA256(String data, String salt) throws NoSuchAlgorithmException {
        return encode(data, salt, "SHA-256");
    }

    public static String encode(String data, String salt, String algo) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algo);
        md.update(data.getBytes());
        if (salt!=null) md.update(salt.getBytes());
        return toHex(md.digest());
    }

    public static String copy(InputStream in, OutputStream out, int blocksize) throws IOException, NoSuchAlgorithmException {
        return copy(in,out,blocksize, DEFAULT_ALGO);
    }

    public static String copy(InputStream in, OutputStream out, int blocksize, String algorithm) throws IOException, NoSuchAlgorithmException {
        if (in.markSupported()) in.reset();
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[blocksize];

        while( in.read(buffer)>0 ) {
            md.update(buffer);
            out.write(buffer);
            out.flush();
        }
        return toHex(md.digest());
    }

    private static String toHex(byte[] data) {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<data.length; i++)
        {
            if (i % 4 == 0 && i != 0)
                buffer.append("");
            int x = (int) data[i];
            if (x<0)
                x+=256;
            if (x<16)
                buffer.append("0");
            buffer.append(Integer.toString(x,16));
        }
        return buffer.toString();
    }
}
