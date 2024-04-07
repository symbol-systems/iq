package systems.symbol.llm;

public class DefaultLLConfig implements I_LLMConfig {

private final String url;
private final String modelName;
private final float frequencyPenalty;
private final float temperature;
private final int maxTokens;
private final float topP;
private final int n, seed;

// Constructor
public DefaultLLConfig(String url, String modelName, int maxTokens, float frequencyPenalty, float temperature, float topP, int n) {
this.url = url;
this.modelName = modelName;
this.temperature = temperature;
this.frequencyPenalty = frequencyPenalty;
this.maxTokens = maxTokens;
this.topP = topP;
this.n = n;
this.seed = 0;
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

@Override
public String getURL() {
return url;
}

@Override
public String getName() {
return modelName;
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
