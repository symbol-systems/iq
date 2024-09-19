package systems.symbol.lake.crawl;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;

import java.util.ArrayList;
import java.util.Collection;

public class HREFFileSystemProvider extends AbstractOriginatingFileProvider {
public HREFFileSystemProvider() {
}

@Override
protected FileSystem doCreateFileSystem(FileName rootFileName, FileSystemOptions fileSystemOptions)
throws FileSystemException {
return new HREFFileSystem(rootFileName, null, fileSystemOptions);
}

@Override
public Collection<Capability> getCapabilities() {
return new ArrayList<>();
}
}
