package systems.symbol.mcp.prompt;

import systems.symbol.mcp.I_MCPPrompt;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * ComposeQueryPrompt — teaches the LLM to write SPARQL over the IQ graph.
 *
 * <p>Template is loaded from {@code assets/prompt/compose_query.txt} on the
 * classpath.  Variables {@code {{goal}}} and {@code {{realm}}} are substituted
 * at render time, following the same {@code {{key}}} convention used across
 * the IQ platform (see {@code assets/prompt/default.txt} in iq-intents).
 */
public class ComposeQueryPrompt implements I_MCPPrompt {

private static final String TEMPLATE_PATH = "assets/prompt/compose_query.txt";

@Override
public String getName() { return "compose_query"; }

@Override
public String getDescription() {
return "Generates a SPARQL query template for a given information retrieval goal " +
   "over the IQ knowledge graph. Call this before sparql.query when you need schema guidance.";
}

@Override
public List<Map<String, Object>> getArguments() {
return List.of(
Map.of("name", "goal",  "description", "What information should the query retrieve?", "required", true),
Map.of("name", "realm", "description", "Target realm (use iq://self/realms to list all)", "required", false)
);
}

@Override
public I_MCPResult render(MCPCallContext ctx, Map<String, String> args) throws MCPException {
String goal  = args.getOrDefault("goal",  "retrieve all entities of the main types");
String realm = args.getOrDefault("realm", "default");

String text = loadTemplate()
.replace("{{goal}}",  goal)
.replace("{{realm}}", realm);

return MCPResult.okText(text);
}

// -------------------------------------------------------------------------

private static String loadTemplate() throws MCPException {
InputStream in = Thread.currentThread()
.getContextClassLoader()
.getResourceAsStream(TEMPLATE_PATH);
if (in == null) {
throw MCPException.internal("Prompt template not found on classpath: " + TEMPLATE_PATH, null);
}
try (in) {
return new String(in.readAllBytes(), StandardCharsets.UTF_8);
} catch (IOException e) {
throw MCPException.internal("Failed to read prompt template: " + e.getMessage(), e);
}
}
}
