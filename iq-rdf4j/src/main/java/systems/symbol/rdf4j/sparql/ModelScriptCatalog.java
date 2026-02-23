package systems.symbol.rdf4j.sparql;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.platform.I_Contents;

public class ModelScriptCatalog implements I_Contents {
Model model;
public ModelScriptCatalog(Model model) {
this.model = model;
}
@Override
public Literal getContent(Resource subject, IRI datatype) {
return IQScripts.findScript(model, subject, datatype, null);
}
}
