package systems.symbol.model;

import org.eclipse.rdf4j.model.IRI;

import java.util.Collection;
import java.util.Map;

public interface HasModels<T> {
    public Collection<T> models(IRI modelSet, Map fields);
    public T model(IRI modelSet, Map fields);
}
