package systems.symbol.vfs;

import com.github.vfss3.S3FileProvider;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.cache.DefaultFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.http.HttpFileProvider;
import org.apache.commons.vfs2.provider.https.HttpsFileProvider;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;

import java.io.File;

public class MyVFS extends DefaultFileSystemManager implements I_FileSystemFactory {

public MyVFS() throws FileSystemException {
init();
}

@Override
public org.apache.commons.vfs2.FileSystemManager create() throws Exception {
return new MyVFS();
}

public void init() throws FileSystemException {
setFilesCache(new DefaultFilesCache());
setCacheStrategy(CacheStrategy.ON_RESOLVE);
addProvider("href", new HREFFileSystemProvider());
addProvider("http", new HttpFileProvider());
addProvider("https", new HttpsFileProvider());
addProvider("s3", new S3FileProvider());
addProvider("file", new DefaultLocalFileProvider());

super.init();
setBaseFile(new File(".iq"));
}

}
