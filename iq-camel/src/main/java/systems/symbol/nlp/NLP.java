package systems.symbol.nlp;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;

public class NLP {
public NLP() {
}

public Model parse(String text) {
return new DynamicModelFactory().createEmptyModel();
}
}
