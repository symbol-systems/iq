package systems.symbol.kernel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Marker annotation to indicate that a field is eligible for RDF injection via {@link systems.symbol.platform.Poke}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pokeable {
}
