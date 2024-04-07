package systems.symbol.research;

import systems.symbol.lake.crawl.VFS;
import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.ns.COMMONS;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResearchSitemapTest {
ValueFactory vf = SimpleValueFactory.getInstance();

@Test
void parseSite() {
}

@Test
void parseSitemap() throws FileSystemException {
//ResearchSitemap research = new ResearchSitemap(new VFS());
//Set<IRI> done = research.execute(vf.createIRI(COMMONS.GG_TEST), vf.createIRI("https://sitemaps.org"));
//assert null != done;
//assert !done.isEmpty();
}
}