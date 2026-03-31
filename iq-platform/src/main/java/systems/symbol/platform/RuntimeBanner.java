package systems.symbol.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RuntimeBanner — centralized startup banner printing.
 *
 * <p>Used by CLI and server entry points to display consistent
 * version/realm/endpoint information at startup.
 */
public class RuntimeBanner {

    private static final Logger log = LoggerFactory.getLogger(RuntimeBanner.class);

    private RuntimeBanner() {
        // utility class
    }

    /**
     * Print startup banner with version, realm, and MCP endpoint.
     */
    public static void print() {
        try {
            String version = I_Self.version();
            String realm = I_Self.name();
            String mcpUrl = System.getenv().getOrDefault("IQ_MCP_URL", "http://localhost:8080/mcp");

            System.out.println("\n║ version: " + version);
            System.out.println("║ realm: " + realm);
            System.out.println("║ mcp: " + mcpUrl + "\n");
        } catch (Exception e) {
            log.debug("Unable to resolve version from manifest", e);
            // Fallback if I_Self is unavailable
            System.out.println("\n║ version: unknown");
            System.out.println("║ realm: unknown");
            System.out.println("║ mcp: http://localhost:8080/mcp\n");
        }
    }
}
