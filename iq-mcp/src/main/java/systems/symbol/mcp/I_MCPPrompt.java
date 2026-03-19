package systems.symbol.mcp;

import java.util.List;
import java.util.Map;

/**
 * I_MCPPrompt — MCP prompt template provider SPI.
 *
 * Prompts are parameterized templates that an LLM can use to refine tool inputs,
 * generate responses, or perform other LLM-driven tasks.
 */
public interface I_MCPPrompt {

/**
 * Unique name of the prompt template.
 */
String getName();

/**
 * Human-readable description shown in the MCP manifest.
 */
String getDescription();

/**
 * List of argument schemas this prompt accepts.
 * Each entry is a map with keys like {@code name}, {@code type}, {@code description}.
 */
List<Map<String, Object>> getArguments();

/**
 * Render the prompt template with the given arguments.
 *
 * @param ctx   the call context (principal, realm)
 * @param args  argument key-value pairs (keys must match argument names from {@link #getArguments()})
 * @return  rendered prompt text wrapped in a non-null result
 * @throws MCPException on rendering or authorization failure
 */
I_MCPResult render(MCPCallContext ctx, Map<String, String> args) throws MCPException;
}
