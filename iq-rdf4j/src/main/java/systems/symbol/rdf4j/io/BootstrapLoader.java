package systems.symbol.rdf4j.io;
/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.io.StreamCopy;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.rdf4j.util.SupportedScripts;
import systems.symbol.util.Stopwatch;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Map;
public class BootstrapLoader implements I_Self {
private final Logger log = LoggerFactory.getLogger(getClass());
public boolean VERBOSE = false;
	private ValueFactory vf = null;
	private RepositoryConnection connection = null;
	private IRI context = null;
	private long since = 0L;
	private boolean deployRDF = true, forceDeployRDF = true, deployAssets = true;
	private boolean fastFail = true;
	public long total_files = 0, total_errors = 0, total_asset_files = 0, total_rdf_files = 0;
	public int commitBuffer = 10000;
	private final int largeFileSize = 10000000;
	Map<String, String> mimes = SupportedScripts.getScriptMimeTypes();
	public BootstrapLoader(String context, RepositoryConnection connection) throws RepositoryException {
init(context,connection);
}

	public BootstrapLoader(String context, RepositoryConnection connection, boolean cleanUp, boolean deployRDF, boolean deployAssets, boolean forceDeployRDF) throws RepositoryException {
		init(context,connection);
		if (cleanUp) clean();
		this.deployRDF=deployRDF;
		this.deployAssets = deployAssets;
		this.forceDeployRDF = forceDeployRDF;
	}

	public BootstrapLoader(IQ iq) throws RepositoryException {
		init(iq.getSelf(),iq.getConnection());
	}

	public void init(String context, RepositoryConnection connection) {
		init( connection.getValueFactory().createIRI(context), connection);
	}

	public void init(IRI context, RepositoryConnection connection)  {
	assert context!=null;
	assert connection!=null;
	this.context = context;

//	supportedScripts  = new SupportedScripts();
//		supportedScripts.supportSPARQL();
//		log.info("scripts: " + supportedScripts.getTypes());

	vf = connection.getValueFactory();

		setConnection(connection);
		ParserConfig parserConfig = new ParserConfig();// new ParserConfig(false, true, false, RDFParser.DatatypeHandling.NORMALIZE)
		connection.setParserConfig(parserConfig);
//		log.info("scripts: " + supportedScripts.getTypes());
}

	public void clean()  {
		getConnection().clear(this.context);
		getConnection().commit();
		RDFPrefixer.defaultNamespaces(getConnection());
	}

	public void deploy(File fileOrFolder) throws IOException, RepositoryException {
		long startTime = System.currentTimeMillis();
		log.info("deploy: " + fileOrFolder.getAbsolutePath()+", exists: "+fileOrFolder.exists());
	if (!fileOrFolder.exists()) return;
		if (fileOrFolder.isDirectory()) {
			findAllFiles(fileOrFolder, fileOrFolder, true);
		} else {
			deployFile(fileOrFolder.getParentFile(), fileOrFolder);
		}
		long elapsedTime = (System.currentTimeMillis()-startTime)/1000;
		log.info("done: " + total_files+" in "+elapsedTime+"s (rdf: "+total_rdf_files+", assets: "+total_asset_files+") errors: "+total_errors);
	}


	protected void findAllFiles(File home, File dir, boolean recurse) throws IOException {
		File[] files = dir.listFiles();
		if(files==null) throw new IOException("Directory listing failed: "+dir.getAbsolutePath());
//		if (VERBOSE)
		log.info("deploy.folder: "+dir.getAbsolutePath() + " & files: x" + files.length);

		// files first
for (File file : files) {
if (file.isDirectory() || file.getName().startsWith(".")) {
// ignore
} else if (file.isFile()) {
deployFile(home, file);
}
}
		// folders second
for (File file : files) {
if (file.isFile() || file.getName().startsWith(".")) {
// ignore .dot files
} else if (file.isDirectory() && recurse) {
findAllFiles(home, file, recurse);
}
}
	}

	private void deployFile(File home, File file) throws IOException {
		if (!isChanged(file)) {
			//if (VERBOSE)
			 log.info("not-modified: {}", file.getAbsolutePath());
			return;
		}
		String name = file.getName();
		RDFFormat format = Rio.getWriterFormatForFileName(name).orElse(null);
		IRI mediatype = FileFormats.toMime(name);
		if ( mediatype==null && format == null) {
			log.warn("deploy.skipped: {}", file.getAbsoluteFile());
			return;
		}

		IRI iri = Files.toIRI(vf, getSelf(), home, file);
		if (iri==null) return;

		if (name.contains(".$.")) {
			mediatype = mediatype==null? Values.iri("urn:"+format.getDefaultMIMEType()) : mediatype;
			iri = Values.iri( iri.stringValue().substring(0, iri.stringValue().length()-2));
			format = null;
		}

		log.info("deploy.file: {} @ {} -> {} --> {}", mediatype==null?format:mediatype, iri, file.getAbsolutePath(), file.length());
		log.info("deploy.file: {} @ {} -> {} --> {}", mediatype==null?format:mediatype, iri, file.getAbsolutePath(), file.length());

		FileInputStream inStream = new FileInputStream(file);
		deploy(iri, inStream, mediatype, format);
		inStream.close();
	}

	public void deploy(IRI localPath, InputStream inStream, IRI mime, RDFFormat format) throws IOException {
		if (VERBOSE) log.debug("deploy.mime: {} ->  {} ({},{})", localPath, format==null?mime:format, deployRDF, deployAssets);

		total_files++;
		if (deployRDF && format!=null) {
			if (inStream.available()>largeFileSize && format.hasStandardURI()) {
				deployLargeRDF(localPath, inStream, format, commitBuffer);
			} else deployRDF(localPath, inStream, format);
			total_rdf_files++;
		} else if (deployAssets && format == null) {
			log.info("deploy.asset: {} @ {}", mime, localPath);
			deployAsset(localPath, inStream, mime);
			total_asset_files ++;
		}
	}

	private void deployRDF(IRI scriptIRI, InputStream inStream, @NotNull RDFFormat format) throws IOException {
		getConnection().begin();
		IRI type = format.getStandardURI();
		try {
			if (this.forceDeployRDF || !exists(scriptIRI, type)) {
				getConnection().add(inStream, scriptIRI.stringValue(), format, context);
				if (type!=null) getConnection().add(scriptIRI, RDF.TYPE, type, getSelf());
				if (VERBOSE) log.debug("deploy.rdf.done: "+format.getStandardURI()+": "+scriptIRI+" in: "+ context +" ("+inStream.available()+")");
			}
			else {
				if (VERBOSE) log.debug("deploy.rdf.skip: <"+scriptIRI+"> a <"+type+"> <"+ context+">.");
			}
		} catch(RDFParseException e) {
			total_errors++;
			log.error("deploy.rdf.broken: "+e.getMessage()+" @ "+scriptIRI);
			if (fastFail) throw new IOException(e.getMessage(),e);
		} catch(UnsupportedRDFormatException e) {
			total_errors++;
			log.error("deploy.rdf.invalid: "+e.getMessage()+" @ "+scriptIRI);
			if (fastFail) throw new IOException(e.getMessage(),e);
		}
		getConnection().commit();
	}

	private void deployLargeRDF(IRI scriptIRI, InputStream inStream, @NotNull RDFFormat format, int commitInterval) throws IOException {
		getConnection().begin();
		IRI type = format.getStandardURI();
		if (!this.forceDeployRDF && exists(scriptIRI, type)) {
			log.debug("large.rdf.skip: <" + scriptIRI + "> a <" + type + "> <" + context + ">.");
			return;
		}

		int linesRead = 0;
		Stopwatch stopwatch = new Stopwatch();
try (BufferedReader reader = new BufferedReader(new InputStreamReader(inStream))) {
String line;
while ((line = reader.readLine()) != null) {
				getConnection().add(new StringReader(line), scriptIRI.stringValue(), format, context);
if (linesRead % commitInterval == 1) {
					log.info("large.rdf.commit: #{} -> {} @ {}s = {}ms", stopwatch.lap(), linesRead, stopwatch.getTotalTime()/1000, stopwatch.mark() );
getConnection().commit();
getConnection().begin();
}
linesRead++;
}
} catch (RDFParseException e) {
total_errors++;
log.error("large.rdf.broken: " + e.getMessage() + " @ " + scriptIRI);
if (fastFail) throw new IOException(e.getMessage(), e);
} catch (UnsupportedRDFormatException e) {
total_errors++;
log.error("large.rdf.invalid: " + e.getMessage() + " @ " + scriptIRI);
if (fastFail) throw new IOException(e.getMessage(), e);
} finally {
getConnection().commit();
}
		log.info("large.rdf.done: {} @ {}/s", stopwatch.summary(), linesRead/(stopwatch.getTotalTime()/1000));
	}
	protected void deployAsset(IRI scriptIRI, InputStream inStream, IRI mimeType) throws IOException, RDFParseException, RepositoryException {
		getConnection().begin();
		String script = StreamCopy.toString(inStream);

		Literal scriptBody = vf.createLiteral(script, mimeType);
		content(scriptIRI, scriptBody);
		getConnection().commit();
	}
	public ClassLoader loadClasses(URL zipFile, ClassLoader classLoader) {
		URL[] jars = new URL[1];
		jars[0] = zipFile;
		return new URLClassLoader (jars, classLoader);
	}

	public RepositoryConnection getConnection() {
		return connection;
	}

 	public void setConnection(RepositoryConnection connection) throws RepositoryException {
		this.connection = connection;
		this.vf = connection.getValueFactory();
	}

	public long getSince() {
		return since;
	}

	public void setSince(long since) {
		this.since = since;
	}

	public boolean isChanged(File file) {
		return !(since>0 && file.lastModified()<=since);
	}

	public void setSince(Date since) {
		this.since = since.getTime();
	}

	private void label(IRI iri, String label) {
		getConnection().add(iri, RDFS.LABEL, vf.createLiteral(label), getSelf());
	}

//	private void mimetype(IRI iri, IRI type) {
//		getConnection().add(iri, DCTERMS.HAS_FORMAT, type, getIdentity());
//	}


	private boolean exists(IRI iri, IRI type) {
		return getConnection().hasStatement(iri, RDF.TYPE, type, false, getSelf());
	}
	
	private void content(IRI scriptIRI, Literal scriptBody) {
		// idempotent
		getConnection().remove(scriptIRI, IQScriptCatalog.HAS_CONTENT, null, getSelf());
		getConnection().add( scriptIRI, IQScriptCatalog.HAS_CONTENT, scriptBody, getSelf());
	}

	public void close() {
		this.connection.close();
	}

	@Override
	public IRI getSelf() {
		return context;
	}
}
