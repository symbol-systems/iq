package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The {@code I_Self} interface represents entities that can self-identify.
 * Implementations must return the canonical IRI for the entity.
 */
public interface I_Self {

        public static String CODENAME = "IQ";
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

        static I_Self self() {
            return () -> Values.iri("urn:"+System.getenv("MY_IQ").toLowerCase());
        }


        static String name() {
                return System.getenv("MY_IQ") == null ? "X.IQ" : System.getenv("MY_IQ").toUpperCase().substring(4);
        }

        static boolean trust() {
                return trust(name());
        }

        static boolean trust(String name) {
                return name.length()>3 && name.startsWith(name()) && !name.substring(0, name.length()+1).contains(":") && !name.contains("{");
        }

        static boolean trust(I_Self self) {
                return self.getSelf().stringValue().startsWith(name()) &&
                        trust(self.getSelf().stringValue());
        }

        static boolean trust(IRI self) {
                return self.stringValue().startsWith(name()) &&
                        trust(self.stringValue());
        }
}
