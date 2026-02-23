package systems.symbol.io;

/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * ProjectArchiver (c) 2013
 * Module: com.projectArchiver.io
 * 
 * @author Symbol Systems
 * Date : 14/01/2014
 * Time : 12:57 AM
 */
public class JarArchiver {
private static final Logger log = LoggerFactory.getLogger(JarArchiver.class);
JarOutputStream jarOutputStream = null;
Manifest manifest = new Manifest();

public JarArchiver(File file) throws IOException {
open(file);
}

public void open(File file) throws IOException {
addAttribute(Attributes.Name.MANIFEST_VERSION, "1.0");
addAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, "Scorpio4");
// addAttribute(Attributes.Name.IMPLEMENTATION_VENDOR_ID,
// file.toURI().toString());
jarOutputStream = new JarOutputStream(new FileOutputStream(file), manifest);
}

public void addAttribute(Attributes.Name name, String value) {
manifest.getMainAttributes().put(name, value);
}

public void addAttribute(String name, String value) {
manifest.getMainAttributes().put(new Attributes.Name(name), value);
}

public String add(InputStream inputStream, String filename, String comment, long size)
throws IOException, NoSuchAlgorithmException {
if (jarOutputStream == null)
throw new IOException("not open");
JarEntry entry = new JarEntry(filename);
entry.setSize(size);

entry.setMethod(JarEntry.DEFLATED); // compressed
entry.setTime(System.currentTimeMillis());
if (comment != null)
entry.setComment(comment);
jarOutputStream.putNextEntry(entry);

// copy I/O & return SHA-1 fingerprint
String fingerprint = Fingerprint.copy(inputStream, jarOutputStream, 4096);
IOUtils.copy(inputStream, jarOutputStream);
log.debug("JAR add " + size + " bytes: " + filename + " -> " + fingerprint);

jarOutputStream.closeEntry();
// jarOutputStream.finish();
return fingerprint;
}

public void close() throws IOException {
if (jarOutputStream == null)
throw new IOException("not open");
jarOutputStream.close();
jarOutputStream = null;
}

}
