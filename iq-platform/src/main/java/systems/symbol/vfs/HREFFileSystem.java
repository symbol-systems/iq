package systems.symbol.vfs;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HREFFileSystem extends AbstractFileSystem {

protected HREFFileSystem(FileName rootFileName, FileObject parentLayer, FileSystemOptions fileSystemOptions) {
super(rootFileName, parentLayer, fileSystemOptions);
}

@Override
protected FileObject createFile(AbstractFileName file) throws FileSystemException {
if (!file.getScheme().startsWith("http")) {
throw new FileSystemException(getClass().getName() + " only supports http(s): " + file.getFriendlyURI());
}
try {
return new HREFFileObject(file, this);
} catch (IOException e) {
throw new FileSystemException(e);
}
}

@Override
protected void addCapabilities(Collection<Capability> caps) {
// intentionally empty
}
}

class HREFFileObject extends AbstractFileObject<HREFFileSystem> {

HREFFileObject(AbstractFileName file, HREFFileSystem fs) throws IOException {
super(file, fs);
deference();
}

void deference() throws IOException {
extractLinks(getURI());
}

@Override
protected long doGetContentSize() throws Exception {
return 0;
}

@Override
protected FileType doGetType() throws Exception {
return null;
}

protected String[] doListChildren() throws Exception {
return extractLinks(getURI());
}

private String[] extractLinks(URI uri) throws IOException {
Document document = Jsoup.connect(uri.toASCIIString()).get();
Elements links = document.select("a[href]");

List<String> linkList = new ArrayList<>();
for (Element link : links) {
linkList.add(link.attr("abs:href"));
}

return linkList.toArray(new String[0]);
}
};