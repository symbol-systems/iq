package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

import java.io.File;
import java.util.Locale;
import java.util.***REMOVED***.Pattern;

import static java.util.***REMOVED***.Pattern.compile;

public class Files {

public static  File toFile(File home, IRI _ns, IRI _iri) {
String iri = _iri.stringValue();
String ns = _ns.stringValue();
if (!iri.startsWith(ns)) return null;
String local = iri.substring(ns.length());
return new File(home,local);
}

static Pattern any_slash = compile("(?<![:/])[/\\\\]+");

public static IRI toIRI(ValueFactory vf, IRI baseIRI, File parentFile, File file) {
if (!file.toString().startsWith(parentFile.toString())) return null;
String path = file.getAbsolutePath().substring((parentFile.getAbsolutePath().length()+1));
String iri = (baseIRI + path.toLowerCase(Locale.ROOT)).replaceAll(any_slash.pattern(),"/");
return vf.createIRI(iri);
}

}
