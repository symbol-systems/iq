package systems.symbol.secrets;

import java.io.IOException;

public class MockVault implements I_SecretsStore {

    public MockVault() {}

    @Override
    public I_Secrets getSecrets(String owner) {
        return new I_Secrets() {
            @Override
            public void setSecret(String key, String secret) {
            }

            @Override
            public String getSecret(String key, String agent) throws SecretsException {
                return System.getenv(key);
            }

            @Override
            public void grant(String key, String agent) {
            }

            @Override
            public void revoke(String key, String agent) {
            }

            @Override
            public boolean granted(String key, String agent) {
                return true;
            }
        };
    }

    @Override
    public I_Secrets setSecrets(String owner, I_Secrets secrets) {
        return getSecrets(owner);
    }

    @Override
    public void save() throws IOException {
    }

    @Override
    public void load() throws IOException, ClassNotFoundException {
    }
}
