package systems.symbol.trust;

//import io.cloudevents.CloudEvent;

import org.eclipse.rdf4j.model.IRI;

public interface I_Claim {
public I_Authority getAuthority();

public String getName();

public IRI getID();
public IRI getType();

}
