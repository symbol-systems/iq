package systems.symbol.connect.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test that requires a specific environment variable to be set.
 * Use with @ExtendWith(RequiresEnvironmentVariableExtension.class).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresEnvironmentVariable {
/**
 * Environment variable name that must be set.
 */
String value();

/**
 * Optional skip message.
 */
String message() default "";
}
