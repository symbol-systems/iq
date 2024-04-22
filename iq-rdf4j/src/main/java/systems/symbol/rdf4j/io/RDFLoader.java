package systems.symbol.rdf4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RDFLoader {
	private static final Logger log = LoggerFactory.getLogger(RDFLoader.class);
RepositoryConnection conn;
ValueFactory vf;
public IRI self, contentPredicate;

public RDFLoader(RepositoryConnection conn) {
connect(conn);
}

	public RDFLoader(RepositoryConnection conn, IRI ns) {
		connect(conn);
		this.self = ns;
	}

	public RDFLoader(RepositoryConnection conn, IRI ns, InputStream in) throws IOException {
connect(conn);
		this.self = ns;
		load(ns, in, RDFFormat.TURTLE);
}

void connect(RepositoryConnection conn) {
this.conn = conn;
	this.vf = conn.getValueFactory();
		contentPredicate = IQScriptCatalog.HAS_CONTENT;
}

	public IRI createIRI(String s) {
		return vf.createIRI(s);
	}
	public Literal content(IRI assetIRI, InputStream inStream, IRI mimeType) throws IOException, RDFParseException, RepositoryException {
		String content = IOCopier.toString(inStream);
		return content(assetIRI,content,mimeType);
	}

	public Literal content(IRI assetIRI, String content) throws IOException, RDFParseException, RepositoryException {
		return content(assetIRI, content, null);
	}

	public Literal content(IRI assetIRI, String content, IRI mimeType) throws IOException, RDFParseException, RepositoryException {
		if (mimeType==null) mimeType = Values.iri("text/plain");
		Literal contentBody = vf.createLiteral(content, mimeType);
		log.debug("content: "+assetIRI + " as " + mimeType.stringValue());
		return content(assetIRI, contentBody);
	}
   
	public Literal content(IRI assetIRI, Literal content) {
		conn.begin();
conn.add( assetIRI, this.contentPredicate, content, assetIRI, self);
		conn.commit();
		return content;
	}

	public void load(RepositoryConnection conn, IRI assetIRI, InputStream tripleStream, RDFFormat format) throws IOException {
		RDFLoader rdfLoader = new RDFLoader(conn);
log.debug("loading: "+ tripleStream.available() +" <- "+format);
rdfLoader.load(assetIRI, tripleStream, format);
log.debug("loaded");
	}

public void load(IRI baseIRI, InputStream inStream, RDFFormat format) throws IOException {
		try {
			conn.begin();
			log.info("load: "+baseIRI);
	conn.add(inStream, baseIRI.stringValue(), format, baseIRI, self );
			conn.commit();
	inStream.close();
			log.info("loaded: "+baseIRI + " as " + format);
		} catch (RDFParseException | RepositoryException | IOException e) {
			log.error("oops: " + e.getMessage()+" @ "+baseIRI, e);
			conn.rollback();
			throw e;
		}
	}

	public void parse(IRI baseIRI, String rdf, RDFFormat format) throws IOException {
		try {
			conn.begin();
			log.info("load: "+baseIRI);
			conn.add(new StringReader(rdf), baseIRI.stringValue(), format, baseIRI, self );
			conn.commit();
			log.info("loaded: "+baseIRI + " as " + format);
		} catch (RDFParseException | RepositoryException | IOException e) {
			log.error("oops: {} @ {}", e.getMessage(), baseIRI, e);
			conn.rollback();
			throw e;
		}
	}
}
