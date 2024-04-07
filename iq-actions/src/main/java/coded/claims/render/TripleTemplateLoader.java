package systems.symbol.render;

import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.AbstractTemplateSource;
import com.github.jknack.handlebars.io.TemplateSource;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.KnowledgeBase;
import systems.symbol.rdf4j.util.TripleFinder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

import static systems.symbol.util.URLHelper.isValidUrl;

public class TripleTemplateLoader extends AbstractTemplateLoader {
private static final Logger log = LoggerFactory.getLogger( TripleTemplateLoader.class );
KnowledgeBase kb;
private final IRI templateMimeType;
private final IRI followPredicate;

public TripleTemplateLoader(KnowledgeBase kb) {
this.kb = kb;
this.templateMimeType = kb.toIRI(COMMONS.MIME_HBS);
this.followPredicate = kb.toIRI( COMMONS.CNT+"chars");
}

protected IRI toIRI(String location) {
// we anticipate a path separator - so manually create IRI instead of kb.toIRI()
ValueFactory vf = kb.getTriples().getValueFactory();
if (isValidUrl(location)) {
return vf.createIRI(location);
} else {
return vf.createIRI(kb.getIdentity().stringValue()+location);
}
}


@Override
public TemplateSource sourceAt(String location) {
IRI templateIRI = toIRI(location);
Set<Literal> ***REMOVED***s = TripleFinder.valuesOf(kb.getTriples(), templateIRI, followPredicate, kb.getIdentity());
// log.trace("template.triples.sourceAt: "+templateIRI+" -> "+***REMOVED***s+" @ "+templateMimeType);
String template = null;
for(Literal l: ***REMOVED***s) {
if (template == null && l.getDatatype().equals(templateMimeType)) {
template = l.stringValue();
}
}
// log.trace("template.triples.found: "+templateIRI+" -> "+template);
if (template==null) return null;
return new TripleTemplateSource(location, template);
}
}
class TripleTemplateSource extends AbstractTemplateSource{
String location, template = null;
long lastModified = 0L;

TripleTemplateSource(String location, String template) {
this.location = location;
this.template = template;
this.lastModified = System.currentTimeMillis();
}
@Override
public String content(Charset charset) throws IOException {
return template;
}

@Override
public String filename() {
return location;
}

@Override
public long lastModified() {
return lastModified;
}
}