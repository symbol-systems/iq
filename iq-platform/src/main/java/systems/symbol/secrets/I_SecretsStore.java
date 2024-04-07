package systems.symbol.secrets;

import java.io.IOException;

public interface I_SecretsStore {

    public I_Secrets getSecrets(String owner);
    public I_Secrets setSecrets(String owner, I_Secrets secrets);

    public void save() throws IOException;
    public void load() throws IOException, ClassNotFoundException;
}
