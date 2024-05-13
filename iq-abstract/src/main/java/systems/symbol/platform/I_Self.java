package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The {@code I_Self} interface represents entities that can self-identify.
 * Implementations must return the canonical IRI for the entity.
 */
public interface I_Self {

/**
 * Returns the canonical IRI representing the entity.
 *
 * @return the canonical IRI for the entity.
 */
IRI getSelf();

/**
 * Retrieves the implementation version from its manifest file.
 *
 * @return the version information of the implementing class.
 * @throws IOException if an I/O error occurs while retrieving the version information.
 */
static String version() throws IOException {
InputStream inputStream = I_Self.class.getResourceAsStream("/META-INF/MANIFEST.MF");
Properties properties = new Properties();
properties.load(inputStream);
return properties.getProperty("Implementation-Version");
}
}
