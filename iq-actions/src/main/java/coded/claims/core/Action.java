package systems.symbol.core;

import systems.symbol.rdf4j.IRIs;
import org.eclipse.rdf4j.model.IRI;

import java.util.Map;

public interface Action {
public IRI getIdentity();

public IRIs supports();
//public IRI execute(IRI intent) throws IQException;
public IRI execute(IRI intent, Map<String, String> model) throws IQException;
}
