package systems.symbol.render;

import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.helper.EachHelper;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.NS;

import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * TripleStore lookup helper that resolves context variables.
 *
 * <pre>
 * {{&gt; (has '.' 'var_name') }}
 * </pre>
 *
 * @author Symbol Systems
 * @since 2.2.0
 */

public class FollowQueryHelper extends EachHelper {
private static final Logger log = LoggerFactory.getLogger(FollowQueryHelper.class);
IQ iq;
SPARQLMapper queries;

public FollowQueryHelper(IQ iq) {
this.iq=iq;
this.queries= new SPARQLMapper(this.iq);
}

//public FollowQueryHelper(IQ kb, HasModels queries) {
//this.iq =kb;
//this.queries=queries;
//}

@Override
public Object apply(Object object, Options options) throws IOException {
if (object == null || options == null || options.params.length < 1) {
log.error("render.follow.missing:"+options.params.length);
return null;
}
if ( !(object instanceof Map) ) {
log.error("render.follow.type:"+object.getClass());
return null;
}
String fieldName = options.params[0].toString();

Map model = (Map)object;
Object that = model.get(NS.KEY_AT_ID);
if (that == null && model.containsKey(fieldName)) {
log.error("render.follow.exists:"+fieldName);
return model.get(fieldName);
}
IRI queryIRI = iq.toIRI("queries/"+fieldName);
System.out.println("render.follow.query: "+that+" @ "+queryIRI);

Collection models = queries.models(queryIRI, model);
System.out.println("render.follow.models: "+fieldName+"-->"+models);
return super.apply(models, options);
}
}
