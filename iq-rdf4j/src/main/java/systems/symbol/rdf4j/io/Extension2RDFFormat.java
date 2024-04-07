package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.rio.RDFFormat;

import java.util.HashMap;

public class Extension2RDFFormat extends HashMap<String,RDFFormat> {
public Extension2RDFFormat() {
put("ttl", RDFFormat.TURTLE);
put("n3", RDFFormat.N3);
put("nt", RDFFormat.NTRIPLES);
put("nq", RDFFormat.NQUADS);
put("rdf", RDFFormat.RDFXML);
}

public RDFFormat getFormat(String name) {
int ix = name.lastIndexOf(".");
if (ix >= 0) {
return get( name.substring(ix+1) );
}
return null;
}
}
