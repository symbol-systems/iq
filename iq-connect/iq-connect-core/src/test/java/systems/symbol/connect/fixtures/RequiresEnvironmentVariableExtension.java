package systems.symbol.connect.fixtures;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

import java.lang.reflect.Method;

/**
 * Extension for @RequiresEnvironmentVariable annotation.
 * Skips tests if required env var is not set.
 */
public class RequiresEnvironmentVariableExtension implements BeforeTestExecutionCallback {

@Override
public void beforeTestExecution(ExtensionContext context) {
Method method = context.getRequiredTestMethod();
RequiresEnvironmentVariable annotation = method.getAnnotation(RequiresEnvironmentVariable.class);

if (annotation != null) {
checkEnvironmentVariable(annotation);
}

Class<?> testClass = context.getTestClass().orElse(null);
if (testClass != null) {
RequiresEnvironmentVariable classAnnotation = testClass.getAnnotation(RequiresEnvironmentVariable.class);
if (classAnnotation != null) {
checkEnvironmentVariable(classAnnotation);
}
}
}

private void checkEnvironmentVariable(RequiresEnvironmentVariable annotation) {
String envVar = annotation.value();
String value = System.getenv(envVar);

if (value == null || value.trim().isEmpty()) {
String message = annotation.message().isEmpty()
? String.format("'%s' not set", envVar)
: annotation.message();
throw new TestAbortedException(message);
}
}
}
