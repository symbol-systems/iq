package systems.symbol.string;
/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ToString {

    public static String toString(File file) throws IOException {
        return toString(new FileReader(file));
    }

    public static String toString(Reader reader) throws IOException {
        StringBuilder buffer = new StringBuilder();
        char[] chars = new char[1024];
        int len = 0;
        while ((len = reader.read(chars)) > 0) {
            buffer.append(chars, 0, len);
        }
        return buffer.toString();
    }

    public static String toString(InputStream stream) throws IOException {
        if (stream == null)
            return null;
        DataInputStream io = new DataInputStream(stream);
        String data = io.readUTF();
        io.close();
        return data;
    }

    public static String toHash(String string, String algo) throws NoSuchAlgorithmException {
        MessageDigest md = null;
        md = MessageDigest.getInstance(algo);
        byte[] digest = md.digest(string.getBytes());
        StringBuilder stringy = new StringBuilder();
        for (byte bcode : digest) {
            stringy.append(String.format("%02x", bcode));
        }
        return stringy.toString();
    }
}
