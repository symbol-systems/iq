package systems.symbol.llm;

public class DefaultLLConfig implements I_LLMConfig {

    public String url;
    public String modelName;
    public float frequencyPenalty;
    public float temperature;
    public int maxTokens;
    public float topP;
    public int n, seed;
    public String secret;

    public DefaultLLConfig(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public DefaultLLConfig(String url, String modelName, int maxTokens) {
        this.url = url;
        this.modelName = modelName;
        this.temperature = 0;
        this.frequencyPenalty = 0;
        this.maxTokens = maxTokens;
        this.topP = 1;
        this.n = 1;
        this.seed = 0;
    }

    public DefaultLLConfig(String url, String modelName, int maxTokens, String secretName) {
        this.url = url;
        this.modelName = modelName;
        this.temperature = 0;
        this.frequencyPenalty = 0;
        this.maxTokens = maxTokens;
        this.topP = 1;
        this.n = 1;
        this.seed = 0;
        this.secret = secretName;
    }

    public String getURL() {
        return url;
    }

    @Override
    public String getName() {
        return modelName;
    }

    @Override
    public String getSecretName() {
        return secret;
    }

    @Override
    public float getFrequencyPenalty() {
        return frequencyPenalty;
    }

    @Override
    public float getTemperature() {
        return temperature;
    }

    @Override
    public int getMaxTokens() {
        return maxTokens;
    }

    @Override
    public float getTopP() {
        return topP;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getSeed() {
        return seed;
    }
}
