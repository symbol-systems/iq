package systems.symbol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code RDF} annotation indicates that a method or type is associated with a IQ identity.
 * This annotation can be used to annotate methods that are executable as IQ operations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RDF {
    /**
     * Specifies the RDF value associated with the annotated method or type.
     *
     * @return the RDF value associated with the annotated method or type.
     */
    String value();
}
