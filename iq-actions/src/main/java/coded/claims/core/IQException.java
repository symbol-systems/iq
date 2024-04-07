package systems.symbol.core;

import systems.symbol.string.PrettyString;
import org.eclipse.rdf4j.model.IRI;

public class IQException extends Exception {
IRI context;

public IQException(IRI context, String message, Exception e) {
super(PrettyString.sanitize(message),e);
this.context = context;
}

public IQException(IRI context, Exception e) {
super(PrettyString.sanitize(e.getMessage()),e);
this.context = context;
}

public IQException(IRI context, String m) {
super(PrettyString.sanitize(m));
this.context = context;
}

public IRI getContext() {
return context;
}
}