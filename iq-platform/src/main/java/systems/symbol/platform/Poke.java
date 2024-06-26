package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class Poke {
protected static final Logger log = LoggerFactory.getLogger(Poke.class);

public static void poke(IRI self, Model model, Object todo) throws IllegalAccessException {
Iterable<Statement> statements = model.filter(self, null, null);
poke(statements, todo);
}

public static void poke(Iterable<Statement> statements, Object todo) throws IllegalAccessException {
for (Statement stmt : statements) {
String fieldName = stmt.getPredicate().getLocalName();
log.info("\tpeek: {} ", fieldName);
try {
Field field  = todo.getClass().getDeclaredField(fieldName);
poke(field, stmt, todo);
} catch (NoSuchFieldException ignored) {
}
}
log.info("peeked: {} ", todo);
}

private static void poke(Field field, Statement stmt, Object config) throws IllegalAccessException {
field.setAccessible(true);

Object value = stmt.getObject();
Class<?> fieldType = field.getType();

log.info("poke: {} -> {} / {}", field.getName(), value, fieldType);
if (fieldType == String.class) {
if (value instanceof Literal) {
field.set(config, ((Literal) value).stringValue());
} else if (value instanceof IRI) {
field.set(config, ((IRI) value).stringValue());
}
} else if (fieldType == int.class) {
field.set(config, Integer.parseInt(((Literal) value).stringValue()));
} else if (fieldType == float.class) {
field.set(config, Float.parseFloat(((Literal) value).stringValue()));
} else if (fieldType == double.class) {
field.set(config, Double.parseDouble(((Literal) value).stringValue()));
} else if (fieldType == boolean.class) {
field.set(config, Boolean.parseBoolean(((Literal) value).stringValue()));
}

}
}

