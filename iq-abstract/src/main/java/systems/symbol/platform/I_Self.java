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
         * @throws IOException if an I/O error occurs while retrieving the version information.
         */
        static String version() throws IOException {
                try (InputStream inputStream = I_Self.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
                        if (inputStream == null) {
                                throw new IOException("MANIFEST.MF file not found: " + I_Self.class.getCanonicalName());
                        }
                        Properties properties = new Properties();
                        properties.load(inputStream);
//                        properties.store(System.out, "MANIFEST.MF");
                        return properties.getProperty("Implementation-Version");
                }
        }


        static I_Self self() {
            return () -> Values.iri(name()+":");
        }


        static String name() {
                return System.getenv("MY_IQ") == null ? CODENAME : System.getenv("MY_IQ");
        }

        static boolean trust(I_Self self) {
                return trust(self.getSelf());
        }

        static boolean trust(IRI self) {
//System.out.println("trust.self: "+self+" --> "+self().getSelf());
                return self.stringValue().startsWith(self().getSelf().stringValue());
        }
}
