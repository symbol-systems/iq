package systems.symbol.rdf4j.fedx.materialization;
import java.util.List;
import java.time.Duration;

public class CachedSchemaMaterializer implements I_SchemaMaterializer {
    public CachedSchemaMaterializer() {}
    
    public CachedSchemaMaterializer(List<I_SourceIntrospector> introspectors, Duration cacheDuration) {
    }
}
