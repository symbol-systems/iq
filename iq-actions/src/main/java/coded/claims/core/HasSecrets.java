package systems.symbol.core;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.util.Map;

public interface HasSecrets {

    public Map secret(String key) throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException;
}
