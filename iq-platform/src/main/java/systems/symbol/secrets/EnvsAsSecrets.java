package systems.symbol.secrets;


public class EnvsAsSecrets implements I_Secrets {
    public String getSecret(String key) throws SecretsException {
        return System.getenv(key);
    }
}
