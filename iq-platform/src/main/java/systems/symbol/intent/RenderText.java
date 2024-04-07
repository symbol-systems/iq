package systems.symbol.intent;

import systems.symbol.annotation.RDF;
import systems.symbol.io.Fingerprint;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.render.HBSRenderer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;

import java.io.IOException;
import java.util.*;

public class RenderText extends RenderAbstract {
protected IRI newPredicate;
boolean force = false;

protected RenderText() {}

public RenderText(IQ iq, Model model, IRI self, IRI templateMime, IRI newPredicate, boolean force) throws IOException {
init(iq, model, self, templateMime);
this.newPredicate = newPredicate;
}

public RenderText(IQ iq, IRI self, IRI templateMime, IRI newPredicate, boolean force) throws IOException {
init(iq, model, self, templateMime);
this.newPredicate = newPredicate;
}


protected void process(IRI entity, String blended) {
Literal ***REMOVED*** = vf.createLiteral(blended, templateMime);
model.remove(entity, newPredicate, null, getIdentity());
model.add(entity, newPredicate, ***REMOVED***, getIdentity());
}

@Override
@RDF(COMMONS.IQ_NS+"render-text")
public Set<IRI> execute(IRI subject, Resource object) {
try {
return perform(subject, object);
} catch (IOException e) {
log.error(e.getLocalizedMessage(), e);
return null;
}
}
}
