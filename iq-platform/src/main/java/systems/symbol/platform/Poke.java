package systems.symbol.platform;

import org.eclipse.rdf4j.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Injects RDF statement values into POJO fields via reflection.
 * 
 * <p>
 * For each RDF statement where the predicate matches a field name,
 * the object value is converted and injected into the field. This enables
 * externalization of configuration to RDF stores.
 * </p>
 * 
 * <h2>Type Support</h2>
 * Supports: String, int, Integer, long, Long, float, Float, double, Double, boolean, Boolean, enums
 * 
 * <h2>Security</h2>
 * IMPORTANT: This class uses reflection to set private fields. Only static methods are exposed,
 * and field access is validated. For future versions, consider restricting to annotated fields only.
 * 
 * <h2>Example</h2>
 * <pre>
 *   class GPTConfig {
 * private String name;
 * private int contextLength;
 *   }
 *   
 *   // RDF: &lt;config&gt; iq:name "gpt-4" ; iq:contextLength 8192 .
 *   Poke.poke(config_iri, model, config);  // name="gpt-4", contextLength=8192
 * </pre>
 * 
 * @author Symbol Systems
 */
public class Poke {
protected static final Logger log = LoggerFactory.getLogger(Poke.class);

public static void poke(Resource self, Model model, Object todo) throws PokeException {
if (model == null) {
throw new PokeException("Model cannot be null");
}
if (todo == null) {
throw new PokeException("Target object cannot be null");
}
Iterable<Statement> statements = model.filter(self, null, null);
poke(statements, todo);
}

public static void poke(Iterable<Statement> statements, Object todo) throws PokeException {
if (statements == null) {
throw new PokeException("Statements cannot be null");
}
if (todo == null) {
throw new PokeException("Target object cannot be null");
}

for (Statement stmt : statements) {
String fieldName = stmt.getPredicate().getLocalName();
log.debug("poke: field={}", fieldName);
try {
Field field = todo.getClass().getDeclaredField(fieldName);
poke(field, stmt, todo);
} catch (NoSuchFieldException ignored) {
log.trace("No field '{}' found in {}", fieldName, todo.getClass().getSimpleName());
} catch (PokeException | IllegalAccessException e) {
if (e instanceof PokeException) {
log.warn("Failed to poke field '{}': {}", fieldName, e.getMessage());
} else {
log.warn("Access denied for field '{}': {}", fieldName, e.getMessage());
}
}
}
log.debug("poked: {}", todo.getClass().getSimpleName());
}

/**
 * Sets a field value via reflection, converting RDF value to target type.
 * 
 * @param field the field to set
 * @param stmt the RDF statement containing the value
 * @param config the object to set the field on
 * @throws PokeException if field type is unsupported or conversion fails
 * @throws IllegalAccessException if field is not accessible
 */
private static void poke(Field field, Statement stmt, Object config) 
throws PokeException, IllegalAccessException {

field.setAccessible(true);

Object value = stmt.getObject();
Class<?> fieldType = field.getType();

try {
Object convertedValue = convertValue(value, fieldType, field.getName());
field.set(config, convertedValue);
log.debug("poked: {} -> {} / {} == {}", field.getName(), value, fieldType, convertedValue);
} catch (NumberFormatException | ClassCastException e) {
throw new PokeException(
String.format("Field conversion failed for '%s' from '%s' to %s", 
field.getName(), value, fieldType.getSimpleName()), 
e);
}
}

/**
 * Converts an RDF value to the target field type.
 * 
 * @param value the RDF value to convert
 * @param fieldType the target field type
 * @param fieldName the field name (for error messages)
 * @return the converted value
 * @throws PokeException if type is unsupported or conversion fails
 */
private static Object convertValue(Object value, Class<?> fieldType, String fieldName) 
throws PokeException {

if (fieldType == String.class) {
return convertToString(value);
} else if (fieldType == int.class || fieldType == Integer.class) {
return Integer.parseInt(extractStringValue(value, fieldName));
} else if (fieldType == long.class || fieldType == Long.class) {
return Long.parseLong(extractStringValue(value, fieldName));
} else if (fieldType == float.class || fieldType == Float.class) {
return Float.parseFloat(extractStringValue(value, fieldName));
} else if (fieldType == double.class || fieldType == Double.class) {
return Double.parseDouble(extractStringValue(value, fieldName));
} else if (fieldType == boolean.class || fieldType == Boolean.class) {
return Boolean.parseBoolean(extractStringValue(value, fieldName));
} else if (fieldType.isEnum()) {
return convertToEnum(value, fieldType, fieldName);
}

throw new PokeException(
String.format("Unsupported field type '%s' for field '%s'. Supported: String, int, long, float, double, boolean, enums", 
fieldType.getSimpleName(), fieldName));
}

/**
 * Extracts string representation from RDF value.
 * 
 * @param value the RDF value
 * @param fieldName the field name (for error context)
 * @return the string value
 * @throws PokeException if value type is unsupported
 */
private static String extractStringValue(Object value, String fieldName) throws PokeException {
if (value instanceof Literal) {
return ((Literal) value).stringValue();
} else if (value instanceof IRI) {
return ((IRI) value).stringValue();
} else {
throw new PokeException(
String.format("Cannot extract string from %s for field '%s'", 
value.getClass().getSimpleName(), fieldName));
}
}

/**
 * Converts RDF value to String.
 * 
 * @param value the RDF value
 * @return the string value
 * @throws PokeException if conversion fails
 */
private static String convertToString(Object value) throws PokeException {
if (value instanceof Literal) {
return ((Literal) value).stringValue();
} else if (value instanceof IRI) {
return ((IRI) value).stringValue();
} else {
throw new PokeException(
String.format("Cannot convert to string from %s", value.getClass().getSimpleName()));
}
}

/**
 * Converts RDF value to enum.
 * 
 * @param value the RDF value
 * @param fieldType the enum class
 * @param fieldName the field name (for error context)
 * @return the enum value
 * @throws PokeException if conversion fails
 */
@SuppressWarnings("unchecked")
private static Object convertToEnum(Object value, Class<?> fieldType, String fieldName) 
throws PokeException {
String stringValue = extractStringValue(value, fieldName);
try {
return Enum.valueOf((Class<? extends Enum>) fieldType, stringValue);
} catch (IllegalArgumentException e) {
throw new PokeException(
String.format("Invalid enum value '%s' for field '%s' of type %s", 
stringValue, fieldName, fieldType.getSimpleName()), 
e);
}
}

/**
 * Exception thrown during RDF value injection.
 * Provides clear error messages for debugging configuration issues.
 */
public static class PokeException extends Exception {
public PokeException(String message) {
super(message);
}

public PokeException(String message, Throwable cause) {
super(message, cause);
}
}
}
