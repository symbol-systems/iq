package systems.symbol.llm.gpt;

import systems.symbol.llm.I_LLMConfig;

public class GPTConfig implements I_LLMConfig {

    public String url;
    public String modelName;
    public float frequencyPenalty;
    public float temperature;
    public int maxTokens = 4096;
    public float topP;
    public int n, seed;
    public String secret;
    public String response_format = null;

    public GPTConfig() {
    }

    public GPTConfig(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public GPTConfig(String url, String modelName, int maxTokens) {
        this.url = url;
        this.modelName = modelName;
        this.temperature = 0.0f;
        this.frequencyPenalty = 0.0f;
        this.maxTokens = maxTokens;
        this.topP = 1;
        this.n = 1;
        this.seed = 0;
    }

    public String getURL() {
        return url;
    }

    @Override
    public String getName() {
        return modelName;
    }

    @Override
    public String getResponseFormat() {
        return response_format;
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
