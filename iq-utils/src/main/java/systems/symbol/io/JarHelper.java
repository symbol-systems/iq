package systems.symbol.io;
/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarHelper {
    private static final Logger log = LoggerFactory.getLogger(JarHelper.class);

	public static File toLocalFile(URL url, File dir) throws IOException {
		String filename = url.getFile().substring(url.getFile().lastIndexOf(File.separator));
		return new File(dir, filename);
	}

	public static File toLocalDir(URL url, File dir) throws IOException {
		File file = toLocalFile(url, dir);
		return new File(file.getParentFile(), stripExtension(file.getName()) );
	}

	public static String stripExtension(String txt) {
		if (txt==null) return null;
		int ix = txt.lastIndexOf(".");
		if (ix>0) return txt.substring(0,ix);
		return txt;
	}

	public static List<File> install(URL url, File dir) throws IOException {
		File file = toLocalFile(url, dir);
		if (!file.exists()) return extract(url, dir);
		return null;
	}

	public static List<File> extract(URL url, File dir) throws IOException {
		File file = toLocalFile(url, dir);
		if (file.exists()) file.delete();
		else file.getParentFile().mkdirs();
		copy( url.openStream(), new FileOutputStream(file) );
		return extract(file, dir);
	}

	public static List<File> extract(File file, File dir) throws IOException {
		File destDir = new File(file.getParentFile(), stripExtension(file.getName()) );
		destDir.mkdirs();
		return extract(new JarFile(file), destDir);
	}

	public static List<File> extract(JarFile jarFile, File destDir) throws IOException {
		Enumeration<JarEntry> entries = jarFile.entries();
		List<File> copied = new ArrayList();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			File file = new File(destDir, entry.getName());

			if (entry.getName().startsWith("META-INF")) {
				// do nothing
			} else if (entry.getName().startsWith(".")) {
				// do nothing
//			} else if (entry.getName().equals("com")) {
//				// do nothing
//			} else if (entry.getName().equals("net")) {
//				// do nothing
//			} else if (entry.getName().equals("org")) {
//				// do nothing
//			} else if (entry.getName().endsWith(".class")) {
//				// do nothing
			} else if (entry.isDirectory()) {
				file.mkdirs();
			} else {
				file.getParentFile().mkdirs();
				copy(jarFile.getInputStream(entry), new FileOutputStream(file));
				copied.add(file);
			}
		}
		return copied;
	}

	public static int copy(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytesRead);
		}
		os.flush();
		os.close();
		is.close();
		return bytesRead;
	}

    public static Properties loadProperties(String file) {
        try {
            Properties properties = new Properties();
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            if (in!=null) properties.load(in);
            return properties;
        } catch (IOException e) {
            return null;
        }
    }

	// Thanks to Greg Briggs (http://www.uofr.net/~greg/java/get-resource-listing.html)

	public static String[] getEntries(String path, ClassLoader classes) throws URISyntaxException, IOException {
		URL dirURL = classes.getResource(path);
		if (dirURL != null && dirURL.getProtocol().equals("file")) {
        /* A file path: easy enough */
			return getEntries(path, new File(dirURL.toURI()));

		}

		if (dirURL == null) {
        /*
         * In case of a jar file, we can't actually find a directory.
         * Have to assume the same jar as clazz.
         */
			dirURL = classes.getResource(path);
		}

		if (dirURL!=null && dirURL.getProtocol().equals("jar")) {
			return getEntries(path, dirURL);
		}
		return null;
	}

	private static String[] getEntries(String uri, File root) {
		Set<String> paths = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
		log.debug("getEntries [file]: "+uri+" in "+root.getAbsolutePath());

		File[] files = root.listFiles();
		for(File file: files) {
			String path = uri.endsWith("/")?uri+file.getName():uri+"/"+file.getName();
			if (file.isFile()) {
				paths.add(path);
			} else if (file.isDirectory()) {
				String[] entries = getEntries(path, file);
				if (entries!=null) {
					for(String entry: entries) {
						paths.add(entry);
					}
				}
			}
		}
		return paths.toArray(new String[paths.size()]);
	}

	public static String[] getEntries(String path, URL dirURL) throws IOException {
		String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
		log.debug("getEntries [url]: "+path+" in "+jarPath);
		JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
		return getEntries(path, jar);

	}

	public static String[] getEntries(String path, JarFile jar) {
		log.debug("getEntries [jar]: "+path+" in "+jar.getName());
		Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
		Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
		while(entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			String name = jarEntry.getName();
			if (name.startsWith(path)) { //filter according to the path
				String entry = name.substring(path.length());
				int checkSubdir = entry.indexOf("/");
				if (checkSubdir >= 0) {
					// if it is a subdirectory, we just return the directory name
					entry = entry.substring(0, checkSubdir);
				}
				result.add(path+"/"+entry);
			}
log.debug("recurseEntries: "+name);
//			String[] entries1 = getEntries(name+"/", jar);
//			for(String entry1: entries1) {
//				result.add(entry1);
//			}
		}
		return result.toArray(new String[result.size()]);
	}
}
