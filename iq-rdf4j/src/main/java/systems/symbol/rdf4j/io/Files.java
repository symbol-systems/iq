package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

import java.io.File;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class Files {

public static Pattern any_slash = compile("(?<![:/])[/\\\\]+");

public static IRI toIRI(ValueFactory vf, IRI baseIRI, File parentFile, File file) {
return toIRI(vf, baseIRI, parentFile, file, "/");
}

public static IRI toIRI(ValueFactory vf, IRI baseIRI, File parentFile, File file, String seperator) {
if (!file.toString().startsWith(parentFile.toString()))
return null;
String path = file.getAbsolutePath().substring((parentFile.getAbsolutePath().length() + 1));
String iri = (baseIRI + path).replaceAll(any_slash.pattern(), seperator);
int ix = iri.lastIndexOf(".");
if (ix < 0)
return vf.createIRI(iri);
return vf.createIRI(iri.substring(0, ix));
}

}
