package systems.symbol.llm.gpt;

import org.junit.jupiter.api.Test;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.llm.JsonOutputSchema;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GPTWrapperSchemaTest {

@Test
void jsonSchemaValidationFailsOnMissingRequiredField() {
GPTConfig config = new GPTConfig("http://localhost", "test", 100);
config.response_format = "json";

GPTWrapper wrapper = new GPTWrapper("token", config);
wrapper.setOutputSchema(new JsonOutputSchema(java.util.List.of("message")));

assertThrows(Exception.class, () -> wrapper.validateResponseSchema("{\"other\":\"x\"}"));
}

@Test
void jsonSchemaValidationPassesWithRequiredField() {
GPTConfig config = new GPTConfig("http://localhost", "test", 100);
config.response_format = "json";

GPTWrapper wrapper = new GPTWrapper("token", config);
wrapper.setOutputSchema(new JsonOutputSchema(java.util.List.of("message")));

assertDoesNotThrow(() -> wrapper.validateResponseSchema("{\"message\":\"ok\"}"));
}

}
