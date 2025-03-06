package systems.symbol.lake;
/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.*;
import systems.symbol.rdf4j.io.FileFormats;
import systems.symbol.rdf4j.io.Files;
import systems.symbol.rdf4j.store.IQStore;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BootstrapVFSLake extends AbstractLake {

public BootstrapVFSLake(String context, RepositoryConnection connection) throws RepositoryException {
init(context, connection);
}

public BootstrapVFSLake(String context, RepositoryConnection connection, boolean cleanUp, boolean deployRDF,
boolean deployAssets, boolean forceDeployRDF) throws RepositoryException {
init(context, connection);
if (cleanUp)
clean();
this.deployRDF = deployRDF;
this.deployAssets = deployAssets;
this.forceDeployRDF = forceDeployRDF;
}

public BootstrapVFSLake(IQStore iq) throws RepositoryException {
init(iq.getSelf(), iq.getConnection());
}

public void deploy(FileObject fileOrFolder) throws IOException, RepositoryException {
long startTime = System.currentTimeMillis();
log.info("lake.deploy: " + fileOrFolder.getPath() + ", exists: " + fileOrFolder.exists());
if (!fileOrFolder.exists())
return;
if (fileOrFolder.isFile())
deployFile(fileOrFolder.getParent(), fileOrFolder);
else
deployFolder(fileOrFolder);
long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
log.info("lake.done: " + total_files + " in " + elapsedTime + "s (rdf: " + total_rdf_files + ", assets: "
+ total_asset_files + ") errors: " + total_errors);
}

protected void deployFolder(FileObject file) throws IOException {
FileObject[] files = file.findFiles(new AllFileSelector());
for (int i = 0; i < files.length; i++)
if (files[i].isFile())
deployFile(file, files[i]);
}

protected void deployFile(FileObject home, FileObject file) throws IOException {
String name = file.getName().getBaseName();
RDFFormat format = Rio.getWriterFormatForFileName(name).orElse(null);
IRI mediatype = FileFormats.toMime(name);
log.debug("lake.file: {} @ {} --> {} & {}", name, file.getPath(), format, mediatype);
if (mediatype == null && format == null) {
log.warn("lake.skipped: {}", file.getPublicURIString());
return;
}

IRI iri = toIRI(getSelf(), home, file);
if (iri == null)
return;

if (name.contains(".$.")) {
mediatype = mediatype == null ? Values.iri("urn:" + format.getDefaultMIMEType()) : mediatype;
iri = Values.iri(iri.stringValue().substring(0, iri.stringValue().length() - 2));
format = null;
}

log.debug("lake.stream: {} @ {} -> {} --> {}", iri, file.getPath(), file.getContent().getSize(),
mediatype == null ? format : mediatype.getLocalName());
InputStream inStream = file.getContent().getInputStream();
deploy(iri, inStream, mediatype, format);
inStream.close();
}

public IRI toIRI(IRI baseIRI, FileObject parentFile, FileObject file) {
log.debug("lake.iri: {} -> {} -> {}", baseIRI, parentFile.getPublicURIString(), file.getPublicURIString());
if (!file.getPublicURIString().startsWith(parentFile.getPublicURIString()))
return null;
String path = file.getPublicURIString().substring((parentFile.getPublicURIString().length() + 1));
String iri = (baseIRI + path).replaceAll(Files.any_slash.pattern(), assetPathSeparator);
int ix = iri.lastIndexOf(".");
if (ix < 0)
return Values.iri(iri);
return Values.iri(iri.substring(0, ix));
}

}
