package systems.symbol.lake;
/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.io.StreamCopy;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.rdf4j.util.SupportedScripts;
import systems.symbol.util.Stopwatch;

import java.io.*;
import java.util.Date;
import java.util.Map;

public class AbstractLake implements I_Self {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    public boolean VERBOSE = false;
    protected ValueFactory vf = null;
    protected RepositoryConnection connection = null;
    protected IRI context = null;
    protected long since = 0L;
    protected boolean deployRDF = true, forceDeployRDF = true, deployAssets = true;
    protected boolean fastFail = true;
    public long total_files = 0, total_errors = 0, total_asset_files = 0, total_rdf_files = 0;
    public int commitBuffer = 10000;
    protected final int largeFileSize = 10000000;
    Map<String, String> mimes = SupportedScripts.getScriptMimeTypes();
    protected String assetPathSeparator = "/";

    protected AbstractLake() {
    }

    public AbstractLake(String context, RepositoryConnection connection) throws RepositoryException {
        init(context, connection);
    }

    public AbstractLake(String context, RepositoryConnection connection, boolean cleanUp, boolean deployRDF,
            boolean deployAssets, boolean forceDeployRDF) throws RepositoryException {
        init(context, connection);
        if (cleanUp)
            clean();
        this.deployRDF = deployRDF;
        this.deployAssets = deployAssets;
        this.forceDeployRDF = forceDeployRDF;
    }

    public AbstractLake(IQStore iq) throws RepositoryException {
        init(iq.getSelf(), iq.getConnection());
    }

    public void init(String context, RepositoryConnection connection) {
        init(connection.getValueFactory().createIRI(context), connection);
    }

    public void init(IRI context, RepositoryConnection connection) {
        assert context != null;
        assert connection != null;
        this.context = context;

        // supportedScripts = new SupportedScripts();
        // supportedScripts.supportSPARQL();
        // log.info("scripts: " + supportedScripts.getTypes());

        vf = connection.getValueFactory();

        setConnection(connection);
        ParserConfig parserConfig = new ParserConfig();// new ParserConfig(false, true, false,
                                                       // RDFParser.DatatypeHandling.NORMALIZE)
        connection.setParserConfig(parserConfig);
        // log.info("scripts: " + supportedScripts.getTypes());
    }

    public void clean() {
        getConnection().clear(this.context);
        getConnection().commit();
        RDFPrefixer.defaultNamespaces(getConnection());
    }

    public void useFlatAssetPaths() {
        this.assetPathSeparator = "_";
    }

    public void deploy(IRI localPath, InputStream inStream, IRI mime, RDFFormat format) throws IOException {
        if (VERBOSE)
            log.debug("lake.mime: {} ->  {} ({},{})", localPath, format == null ? mime : format, deployRDF,
                    deployAssets);

        total_files++;
        if (deployRDF && format != null) {
            if (inStream.available() > largeFileSize && format.hasStandardURI()) {
                deployLargeRDF(localPath, inStream, format, commitBuffer);
            } else
                deployRDF(localPath, inStream, format);
            total_rdf_files++;
        } else if (deployAssets && format == null) {
            log.debug("lake.asset: {} @ {}", mime, localPath);
            deployAsset(localPath, inStream, mime);
            total_asset_files++;
        }
    }

    protected void deployRDF(IRI scriptIRI, InputStream inStream, @NotNull RDFFormat format) throws IOException {
        getConnection().begin();
        IRI type = format.getStandardURI();
        try {
            if (this.forceDeployRDF || !exists(scriptIRI, type)) {
                getConnection().add(inStream, scriptIRI.stringValue(), format, context);
                // if (type != null)
                // getConnection().add(scriptIRI, RDF.TYPE, type, getSelf());
                if (VERBOSE)
                    log.debug("lake.rdf.done: " + format.getStandardURI() + ": " + scriptIRI + " in: " + context + " ("
                            + inStream.available() + ")");
            } else {
                if (VERBOSE)
                    log.debug("lake.rdf.skip:  <" + scriptIRI + "> a <" + type + "> <" + context + ">.");
            }
        } catch (RDFParseException e) {
            total_errors++;
            log.error("lake.rdf.broken: {} @ {}", e.getMessage(), scriptIRI);
            if (fastFail)
                throw new IOException(e.getMessage() + " @ " + scriptIRI, e);
        } catch (UnsupportedRDFormatException e) {
            total_errors++;
            log.error("lake.rdf.invalid: {} @ {}", e.getMessage(), scriptIRI);
            if (fastFail)
                throw new IOException(e.getMessage() + " @ " + scriptIRI, e);
        }
        getConnection().commit();
    }

    protected void deployLargeRDF(IRI scriptIRI, InputStream inStream, @NotNull RDFFormat format, int commitInterval)
            throws IOException {
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
                    log.info("large.rdf.commit: #{} -> {} @ {}s = {}ms", stopwatch.elapsed(), linesRead,
                            stopwatch.getTotalTime() / 1000, stopwatch.mark());
                    getConnection().commit();
                    getConnection().begin();
                }
                linesRead++;
            }
        } catch (RDFParseException e) {
            total_errors++;
            log.error("large.rdf.broken: " + e.getMessage() + " @ " + scriptIRI);
            if (fastFail)
                throw new IOException(e.getMessage(), e);
        } catch (UnsupportedRDFormatException e) {
            total_errors++;
            log.error("large.rdf.invalid: " + e.getMessage() + " @ " + scriptIRI);
            if (fastFail)
                throw new IOException(e.getMessage(), e);
        } finally {
            getConnection().commit();
        }
        log.debug("large.rdf.done: {} @ {}/s", stopwatch.summary(), linesRead / (stopwatch.getTotalTime() / 1000));
    }

    protected void deployAsset(IRI scriptIRI, InputStream inStream, IRI mimeType)
            throws IOException, RDFParseException, RepositoryException {
        getConnection().begin();
        String script = StreamCopy.toString(inStream);

        Literal scriptBody = vf.createLiteral(script, mimeType);
        content(scriptIRI, scriptBody);
        getConnection().commit();
    }

    // public ClassLoader loadClasses(URL zipFile, ClassLoader classLoader) {
    // URL[] jars = new URL[1];
    // jars[0] = zipFile;
    // return new URLClassLoader(jars, classLoader);
    // }

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
        return !(since > 0 && file.lastModified() <= since);
    }

    public void setSince(Date since) {
        this.since = since.getTime();
    }

    // protected void label(IRI iri, String label) {
    // getConnection().add(iri, RDFS.LABEL, vf.createLiteral(label), getSelf());
    // }

    // protected void mimetype(IRI iri, IRI type) {
    // getConnection().add(iri, DCTERMS.HAS_FORMAT, type, getIdentity());
    // }

    protected boolean exists(IRI iri, IRI type) {
        return getConnection().hasStatement(iri, RDF.TYPE, type, false, getSelf());
    }

    protected void content(IRI scriptIRI, Literal scriptBody) {
        // idempotent
        getConnection().remove(scriptIRI, IQScriptCatalog.HAS_CONTENT, null, getSelf());
        getConnection().add(scriptIRI, IQScriptCatalog.HAS_CONTENT, scriptBody, getSelf());
    }

    public void close() {
        this.connection.close();
    }

    @Override
    public IRI getSelf() {
        return context;
    }
}
