package systems.symbol.trust;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

import org.apache.commons.vfs2.*;

public class VFSKeyStore extends AbstractKeyStore {
private final FileObject keysHome;

public VFSKeyStore(FileSystemManager fsm, FileObject keysHome) throws Exception {
this.keysHome = keysHome;
if (!this.keysHome.exists()) {
this.keysHome.createFolder();
}
if (!isProvisioned()) {
save(newKeys(algo));
}
}

public boolean isProvisioned() {
try {
FileObject privateKeyFile = keysHome.resolveFile(PRIVATE_KEY_FILENAME);
FileObject publicKeyFile = keysHome.resolveFile(PUBLIC_KEY_FILENAME);
return keysHome.exists() && privateKeyFile.exists() && publicKeyFile.exists();
} catch (Exception e) {
return false;
}
}

// Save KeyPair to a folder
public void save(KeyPair keyPair) throws Exception {
FileObject privateKeyFile = keysHome.resolveFile(PRIVATE_KEY_FILENAME);
FileObject publicKeyFile = keysHome.resolveFile(PUBLIC_KEY_FILENAME);

try (OutputStream privateOut = privateKeyFile.getContent().getOutputStream();
OutputStream publicOut = publicKeyFile.getContent().getOutputStream()) {
privateOut.write(toPKCS8(keyPair.getPrivate()).getBytes());
publicOut.write(toPKCS8(keyPair.getPublic()).getBytes());
}
}

// Load KeyPair from VFS FileObjects
public KeyPair load() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
FileObject privateKeyFile = keysHome.resolveFile(PRIVATE_KEY_FILENAME);
FileObject publicKeyFile = keysHome.resolveFile(PUBLIC_KEY_FILENAME);

try (InputStream privateIn = privateKeyFile.getContent().getInputStream();
InputStream publicIn = publicKeyFile.getContent().getInputStream()) {
return load(privateIn, publicIn);
}
}
}
