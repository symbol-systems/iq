package systems.symbol.rdf4j.io;

import java.io.*;
import java.util.HashMap;

import com.github.jsonldjava.core.JsonLdOptions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.rio.jsonld.JSONLDWriter;

public class RDFDump {
private static final Logger log = LoggerFactory.getLogger(RDFDump.class);
private static DynamicModelFactory dmf = new DynamicModelFactory();

public static WriterConfig getWriterConfig() {
WriterConfig config = new WriterConfig();
config.set(BasicWriterSettings.PRETTY_PRINT, true);
config.set(BasicWriterSettings.BASE_DIRECTIVE, true);
config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
config.set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true);
return config;
}

public static void copyNamespaces(RepositoryConnection conn, Model model) {
conn.getNamespaces().forEach(ns -> {
if (ns.getPrefix().length() > 1) {
log.debug("namespace: [" + ns.getPrefix() + ":] " + ns.getName());
model.setNamespace(ns.getPrefix(), ns.getName());
}
});

}

public static Model dump(RepositoryConnection conn, File to_file, RDFFormat format) throws Exception {
FileOutputStream out = new FileOutputStream(to_file);
Model model = dump(conn, out, format, null);
out.close();
return model;
}

public static Model dump(RepositoryConnection from, OutputStream out, RDFFormat format, IRI baseIRI) throws Exception {
Model model = dmf.createEmptyModel();
copyNamespaces(from, model);
if (baseIRI != null) {
model.setNamespace("", baseIRI.stringValue());
}
from.export(new StatementCollector(model, new HashMap<>()));

WriterConfig config = getWriterConfig();
Rio.write(model, out, format, config);
return model;
}

public static Model dump(RepositoryConnection conn, GraphQueryResult from_result, RDFFormat format, IRI baseIRI) throws Exception {
return dump(conn, from_result, new ByteArrayOutputStream(), format, baseIRI);
}

public static Model dump(RepositoryConnection conn, GraphQueryResult from_result, OutputStream out, RDFFormat format, IRI baseIRI) throws Exception {
// Convert GraphQueryResult to a Model
Model model = dmf.createEmptyModel();
copyNamespaces(conn, model);
if (baseIRI != null) {
model.setNamespace("", baseIRI.stringValue());
}
from_result.forEach(model::add);

WriterConfig config = getWriterConfig();
Rio.write(model, out, format, config);
return model;
}

public static void dump(Model model, OutputStream out, RDFFormat format) throws Exception {
WriterConfig config = getWriterConfig();
Rio.write(model, out, format, config);
}

public static void ld(Model model, OutputStream out) {
WriterConfig writerConfig = getWriterConfig();
writerConfig.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
Rio.write(model, out, RDFFormat.JSONLD, writerConfig);
}

public static Model ld(RepositoryConnection from, OutputStream out, IRI baseIRI) throws Exception {
Model model = dmf.createEmptyModel();
copyNamespaces(from, model);
if (baseIRI != null) {
model.setNamespace("", baseIRI.stringValue());
}
from.export(new StatementCollector(model, new HashMap<>()));

WriterConfig writerConfig = getWriterConfig();
writerConfig.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
Rio.write(model, out, RDFFormat.JSONLD, writerConfig);
return model;
}

public static void dump(Model model) throws Exception {
dump(model, System.out, RDFFormat.TURTLE);
}

}
