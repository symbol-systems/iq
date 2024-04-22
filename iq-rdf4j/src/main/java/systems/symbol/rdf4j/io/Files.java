package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class Files {

    static Pattern any_slash = compile("(?<![:/])[/\\\\]+");

    public static IRI toIRI(ValueFactory vf, IRI baseIRI, File parentFile, File file) {
        if (!file.toString().startsWith(parentFile.toString())) return null;
        String path = file.getAbsolutePath().substring((parentFile.getAbsolutePath().length()+1));
        String iri = (baseIRI + path.toLowerCase(Locale.ROOT)).replaceAll(any_slash.pattern(),"/");
        int ix = iri.lastIndexOf(".");
        if (ix<0) return vf.createIRI(iri);
        return vf.createIRI(iri.substring(0,ix));
    }

}
