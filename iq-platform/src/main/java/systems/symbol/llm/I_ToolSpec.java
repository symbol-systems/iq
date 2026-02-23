package systems.symbol.llm;

import java.util.Map;

import systems.symbol.llm.tools.FuncToolParam;

public interface I_ToolSpec {

    /**
     * Gets the name of the LLM function.
     * 
     * @return the name of the function
     */
    String getName();

    /**
     * Gets the description of what the function does.
     * 
     * @return the function's description
     */
    String getDescription();

    /**
     * Gets the parameters required for the function call.
     * The parameters should follow a structured format, e.g., in JSON.
     * 
     * @return a map of parameter names to their descriptions
     */
    Map<String, FuncToolParam> getParameters();

    /**
     * Specifies the return type of the function.
     * 
     * @return the return type of the function (e.g., "JSON", "String", etc.)
     */
    String getReturnType();

    /**
     * Returns error handling information for the function.
     * 
     * @return a description of error handling for this function
     */
    String getErrorHandling();
}
