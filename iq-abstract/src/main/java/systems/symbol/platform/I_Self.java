package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

/**
 * The {@code I_Self} interface represents entities that can self-identify.
 * Implementations must return the canonical IRI for the entity.
 */
public interface I_Self {
/**
 * The canonical entity known as IRI .
 *
 * @return the canonical IRI for the entity.
 */
IRI getSelf();

String CODENAME = "iq";

/**
 * Retrieves the implementation version from its manifest file.
 *
 * @return the version information of the implementing class.
 * @throws IOException if an I/O error occurs while retrieving the version
 * information.
 */
public static String version() throws IOException {
try {
// Construct the correct jar URI
URI uri = I_Self.class.getResource("/META-INF/MANIFEST.MF").toURI();

// If running from within a JAR, convert the URI to
// jar:file:/path/to/jar!/META-INF/MANIFEST.MF
if (uri.getScheme().equals("jar")) {
// No need to change the URI, it's already in jar scheme
try (InputStream inputStream = I_Self.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
if (inputStream == null) {
throw new IOException("MANIFEST.MF file not found: " + I_Self.class.getCanonicalName());
}

Properties properties = new Properties();
properties.load(inputStream);
return properties.getProperty("Implementation-Version");
}
} else {
// It's not running inside a jar, so we need to open a FileSystem
String jarUri = "jar:" + uri.toString();
try (FileSystem fs = FileSystems.newFileSystem(URI.create(jarUri), new HashMap<>())) {
Path manifestPath = fs.getPath("/META-INF/MANIFEST.MF");
try (InputStream inputStream = Files.newInputStream(manifestPath)) {
if (inputStream == null) {
throw new IOException("MANIFEST.MF file not found: " + I_Self.class.getCanonicalName());
}

Properties properties = new Properties();
properties.load(inputStream);
return properties.getProperty("Implementation-Version");
}
}
}
} catch (URISyntaxException e) {
throw new IOException("Error resolving URI for manifest", e);
}
}

static I_Self self() {
return () -> Values.iri(name() + ":");
}

static String name() {
return System.getenv("IQ") == null ? CODENAME : System.getenv("IQ");
}

static boolean trust(I_Self self) {
return trust(self.getSelf());
}

static boolean trust(IRI self) {
// System.out.println("trust.self: "+self+" --> "+self().getSelf());
return self.stringValue().startsWith(self().getSelf().stringValue());
}
}
