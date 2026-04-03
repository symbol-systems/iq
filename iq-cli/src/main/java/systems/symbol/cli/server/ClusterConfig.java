package systems.symbol.cli.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ClusterConfig {

private static final String CLUSTER_FILE_NAME = "cluster.ttl";

private ClusterConfig() {
}

public static Path getClusterFile() throws IOException {
String home = System.getenv("IQ_HOME");
Path homePath = home == null || home.isBlank() ? Paths.get(".iq") : Paths.get(home);

if (!homePath.endsWith(".iq")) {
homePath = homePath.resolve(".iq");
}

if (!Files.exists(homePath)) {
Files.createDirectories(homePath);
}

return homePath.resolve(CLUSTER_FILE_NAME);
}

public static Set<String> readClusterNodes() throws IOException {
Path file = getClusterFile();
if (!Files.exists(file)) {
return new LinkedHashSet<>();
}

List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
return lines.stream()
.map(String::trim)
.filter(s -> !s.isEmpty())
.filter(s -> !s.startsWith("#"))
.collect(Collectors.toCollection(LinkedHashSet::new));
}

public static void writeClusterNodes(Set<String> nodes) throws IOException {
Path file = getClusterFile();
List<String> lines = nodes.stream().sorted().collect(Collectors.toList());
Files.write(file, lines, StandardCharsets.UTF_8);
}

public static boolean isValidNode(String node) {
if (node == null || node.isBlank()) {
return false;
}
try {
URI uri = new URI(node);
return uri.getScheme() != null && !uri.getScheme().isBlank() && uri.getHost() != null && !uri.getHost().isBlank();
} catch (URISyntaxException ex) {
return false;
}
}
}
