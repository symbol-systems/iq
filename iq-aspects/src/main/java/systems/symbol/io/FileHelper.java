package systems.symbol.io;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FileHelper {
    static final Logger log = LoggerFactory.getLogger(FileHelper.class);

    public static String localize(File root, File file) {
        return toRelative(root, file);
    }

    public static String toRelative(File root, File file) {
        if (root == null || file == null || !file.getAbsolutePath().startsWith(root.getAbsolutePath()))
            return null;
        if (root.equals(file))
            return ".";
        return file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);
    }

    public static String toExtension(String txt) {
        if (txt == null)
            return null;
        int ix = txt.lastIndexOf(".");
        if (ix > 0)
            return txt.substring(ix + 1);
        return null;
    }

    public static String stripExtension(String txt) {
        if (txt == null)
            return null;
        int ix = txt.lastIndexOf(".");
        if (ix > 0)
            return txt.substring(0, ix);
        return txt;
    }

    public static File fromQID(File root, String uuid) {
        if (uuid.startsWith("urn:"))
            uuid = uuid.substring(4);
        uuid = uuid.replace(":", File.separator);
        return new File(root, uuid);
    }

    public static String toQID(File root, File file) {
        return toQID(localize(root, file));
    }

    public static Map<String, String> mapContents(File root) throws IOException {
        Map<String, String> files = new HashMap<>();
        mapContents(files, root, root);
        return files;
    }

    public static void mapContents(Map<String, String> files, File root, File file) throws IOException {
        if (file.isFile()) {
            files.put(localize(root, file), StreamCopy.toString(file));
        } else {
            File[] _files = file.listFiles();
            if (_files == null)
                throw new IOException("Directory missing: " + file.getAbsolutePath());
            for (int i = 0; i < _files.length; i++)
                mapContents(files, root, _files[i]);
        }
    }

    public static String getLocalName(String filename) {
        int ix = filename.lastIndexOf("/");
        ix = ix >= 0 ? ix + 1 : filename.lastIndexOf("\\") + 1;
        return filename.substring(ix);
    }

    public static String toQID(String txt) {
        if (txt == null)
            return null;
        return stripExtension(txt).replace("\\/", "::");
    }

    public static File toTodayFile(File home) {
        return toTodayFile(home, new Date());
    }

    public static File toTodayFile(File home, Date now) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd/");
        return toTodayFile(home, now, format);
    }

    public static File toTodayFile(File home, Date now, SimpleDateFormat format) {
        return new File(home, format.format(now));
    }
}
