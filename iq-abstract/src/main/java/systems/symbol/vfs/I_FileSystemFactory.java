package systems.symbol.vfs;

import org.apache.commons.vfs2.FileSystemManager;

public interface I_FileSystemFactory {
FileSystemManager create() throws Exception;
}
