package systems.symbol.lake;
/*
 *  Copyright (c) 2009-2015, 2021-2024 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.*;
import systems.symbol.rdf4j.io.FileFormats;
import systems.symbol.rdf4j.io.Files;
import systems.symbol.rdf4j.store.IQStore;
import java.io.*;

public class BootstrapLake extends AbstractLake {

public BootstrapLake(String context, RepositoryConnection connection) throws RepositoryException {
init(context, connection);
}

public BootstrapLake(String context, RepositoryConnection connection, boolean cleanUp, boolean deployRDF,
boolean deployAssets, boolean forceDeployRDF) throws RepositoryException {
init(context, connection);
if (cleanUp)
clean();
this.deployRDF = deployRDF;
this.deployAssets = deployAssets;
this.forceDeployRDF = forceDeployRDF;
}

public BootstrapLake(IQStore iq) throws RepositoryException {
init(iq.getSelf(), iq.getConnection());
}

public void deploy(File fileOrFolder) throws IOException, RepositoryException {
long startTime = System.currentTimeMillis();
log.info("deploy: " + fileOrFolder.getAbsolutePath() + ", exists: " + fileOrFolder.exists());
if (!fileOrFolder.exists())
return;
if (fileOrFolder.isDirectory()) {
findAllFiles(fileOrFolder, fileOrFolder, true);
} else {
deployFile(fileOrFolder.getParentFile(), fileOrFolder);
}
long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
log.info("done: " + total_files + " in " + elapsedTime + "s (rdf: " + total_rdf_files + ", assets: "
+ total_asset_files + ") errors: " + total_errors);
}

protected void findAllFiles(File home, File dir, boolean recurse) throws IOException {
File[] files = dir.listFiles();
if (files == null)
throw new IOException("Directory listing failed: " + dir.getAbsolutePath());
// if (VERBOSE)
log.debug("lake.folder: " + dir.getAbsolutePath() + " & files: x" + files.length);

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
// if (VERBOSE)
log.info("not-modified: {}", file.getAbsolutePath());
return;
}
String name = file.getName();
RDFFormat format = Rio.getWriterFormatForFileName(name).orElse(null);
IRI mediatype = FileFormats.toMime(name);
if (mediatype == null && format == null) {
log.warn("lake.skipped: {}", file.getAbsoluteFile());
return;
}

IRI iri = Files.toIRI(vf, getSelf(), home, file, assetPathSeparator);
if (iri == null)
return;

if (name.contains(".$.")) {
mediatype = mediatype == null ? Values.iri("urn:" + format.getDefaultMIMEType()) : mediatype;
iri = Values.iri(iri.stringValue().substring(0, iri.stringValue().length() - 2));
format = null;
}

log.info("lake.file: {} @ {} -> {} --> {}", iri, file.getPath(), file.length(),
mediatype == null ? format : mediatype.getLocalName());
FileInputStream inStream = new FileInputStream(file);
deploy(iri, inStream, mediatype, format);
inStream.close();
}

}
