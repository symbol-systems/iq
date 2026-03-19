package systems.symbol.kernel.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads version and build metadata about the running kernel.
 *
 * <p>Resolution order for the implementation version:
 * <ol>
 *   <li>{@code /META-INF/MANIFEST.MF} {@code Implementation-Version} attribute</li>
 *   <li>{@code /build.properties} {@code project.version} property</li>
 *   <li>Fallback: {@code "dev"}</li>
 * </ol>
 */
public final class WorkspaceProbe {

private WorkspaceProbe() {}

/**
 * Returns the running implementation version, or {@code "dev"} if not
 * running from a packaged JAR.
 */
public static String version() {
String v = fromManifest();
if (v != null) return v;
v = fromBuildProperties();
if (v != null) return v;
return "dev";
}

/** Reads {@code Implementation-Version} from {@code META-INF/MANIFEST.MF}. */
public static String fromManifest() {
try (InputStream in = WorkspaceProbe.class
.getResourceAsStream("/META-INF/MANIFEST.MF")) {
if (in == null) return null;
Properties p = new Properties();
p.load(in);
return p.getProperty("Implementation-Version");
} catch (IOException e) {
return null;
}
}

/** Reads {@code project.version} from {@code build.properties}. */
public static String fromBuildProperties() {
try (InputStream in = WorkspaceProbe.class
.getResourceAsStream("/build.properties")) {
if (in == null) return null;
Properties p = new Properties();
p.load(in);
return p.getProperty("project.version");
} catch (IOException e) {
return null;
}
}
}
