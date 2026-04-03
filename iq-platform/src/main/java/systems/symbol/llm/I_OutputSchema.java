package systems.symbol.llm;

import java.util.List;

public interface I_OutputSchema {

/**
 * Validate JSON content, return list of issues (empty = valid).
 */
List<String> validate(String content);

}
