package systems.symbol.llm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.KernelContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manager for LLM provider configuration and defaults.
 * 
 * Persists configuration to `.iq/llm-config.yaml` file:
 * ```yaml
 * default-provider: openai
 * providers:
 *   openai:
 * model: gpt-4-turbo
 * temperature: 0.7
 *   groq:
 * model: mixtral-8x7b
 * temperature: 0.5
 * ```
 * 
 * Also tracks cost information for billing:
 * - Input tokens per request
 * - Output tokens per request
 * - Estimated costs per provider-model pair
 * 
 * @author Symbol Systems
 */
public class LLMConfigManager {
private static final Logger log = LoggerFactory.getLogger(LLMConfigManager.class);
private static final String CONFIG_FILENAME = "llm-config.yaml";

private final File configFile;
private final Map<String, Object> config = new HashMap<>();
private final Map<String, ProviderCosts> costTracking = new HashMap<>();

/**
 * Cost tracking for LLM provider usage.
 */
public static class ProviderCosts {
public String provider;
public String model;
public long totalInputTokens = 0;
public long totalOutputTokens = 0;
public double estimatedCostUSD = 0.0;
public long requestCount = 0;
public long lastUpdated = System.currentTimeMillis();

public ProviderCosts(String provider, String model) {
this.provider = provider;
this.model = model;
}

public void recordUsage(long inputTokens, long outputTokens, double costUSD) {
totalInputTokens += inputTokens;
totalOutputTokens += outputTokens;
estimatedCostUSD += costUSD;
requestCount++;
lastUpdated = System.currentTimeMillis();
}

public double getAverageCostPerRequest() {
return requestCount > 0 ? estimatedCostUSD / requestCount : 0;
}
}

/**
 * Initialize configuration manager.
 * 
 * Loads config from ~/.iq/llm-config.yaml if it exists.
 * 
 * @param kernelContext kernel context for accessing home directory
 */
public LLMConfigManager(KernelContext kernelContext) {
File homeDir = kernelContext.getHome();
this.configFile = new File(homeDir, CONFIG_FILENAME);

log.info("[LLMConfigManager] config file: {}", configFile.getAbsolutePath());

// Load config if exists
if (configFile.exists()) {
loadConfig();
} else {
// Initialize with defaults
initializeDefaults();
}
}

/**
 * Initialize default configuration.
 */
private void initializeDefaults() {
try {
// Create default config structure
Map<String, Object> defaultConfig = new HashMap<>();
defaultConfig.put("default-provider", "openai");
defaultConfig.put("openai.model", "gpt-4-turbo");
defaultConfig.put("openai.temperature", "0.7");
defaultConfig.put("groq.model", "mixtral-8x7b");
defaultConfig.put("groq.temperature", "0.5");

config.putAll(defaultConfig);

log.info("[LLMConfigManager] initialized default configuration");
} catch (Exception e) {
log.error("[LLMConfigManager] error initializing defaults", e);
}
}

/**
 * Load configuration from file.
 */
private void loadConfig() {
try {
String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);

// Simple YAML parsing (line-by-line for this simple config)
for (String line : content.split("\n")) {
line = line.trim();
if (line.isEmpty() || line.startsWith("#")) {
continue;
}

if (line.startsWith("default-provider:")) {
String provider = line.substring("default-provider:".length()).trim();
config.put("default-provider", provider);
}
}

log.info("[LLMConfigManager] loaded configuration from {}", configFile.getName());
} catch (IOException e) {
log.error("[LLMConfigManager] error loading config", e);
initializeDefaults();
}
}

/**
 * Save configuration to file.
 */
public void saveConfig() {
try {
StringBuilder sb = new StringBuilder();
sb.append("# IQ LLM Configuration\n");
sb.append("# Generated: ").append(new Date()).append("\n\n");

// Write all config entries
for (Map.Entry<String, Object> entry : config.entrySet()) {
sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
}

Files.writeString(configFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
log.info("[LLMConfigManager] saved configuration to {}", configFile.getName());
} catch (IOException e) {
log.error("[LLMConfigManager] error saving config", e);
}
}

/**
 * Set the default LLM provider.
 * 
 * Example: setDefaultProvider("groq")
 * 
 * @param provider provider name (openai, groq, etc.)
 */
public void setDefaultProvider(String provider) {
config.put("default-provider", provider);
saveConfig();
log.info("[LLMConfigManager] set default provider: {}", provider);
}

/**
 * Get the current default LLM provider.
 * 
 * @return provider name, or "openai" if not set
 */
public String getDefaultProvider() {
Object provider = config.get("default-provider");
return provider != null ? provider.toString() : "openai";
}

/**
 * Get configuration for a specific provider.
 * 
 * @param provider provider name
 * @return config map with provider settings, or empty map if not found
 */
public Map<String, Object> getProviderConfig(String provider) {
Map<String, Object> result = new HashMap<>();

// Find all config keys starting with "provider."
for (Map.Entry<String, Object> entry : config.entrySet()) {
if (entry.getKey().startsWith(provider + ".")) {
String key = entry.getKey().substring((provider + ".").length());
result.put(key, entry.getValue());
}
}

return result;
}

/**
 * Update configuration for a provider.
 * 
 * @param provider provider name
 * @param key config key
 * @param value config value
 */
public void setProviderConfig(String provider, String key, Object value) {
String configKey = provider + "." + key;
config.put(configKey, value);
saveConfig();
log.info("[LLMConfigManager] set provider config: {}.{} = {}", provider, key, value);
}

/**
 * Record token usage for cost tracking.
 * 
 * @param provider provider name
 * @param model model name
 * @param inputTokens tokens consumed by input
 * @param outputTokens tokens generated in output
 * @param costUSD estimated cost in USD
 */
public void recordTokenUsage(String provider, String model, long inputTokens, long outputTokens, double costUSD) {
String key = provider + ":" + model;
ProviderCosts costs = costTracking.computeIfAbsent(key, k -> new ProviderCosts(provider, model));
costs.recordUsage(inputTokens, outputTokens, costUSD);

log.debug("[LLMConfigManager] recorded usage: {} {} - {} in, {} out, ${}", 
provider, model, inputTokens, outputTokens, costUSD);
}

/**
 * Get cost summary for all providers.
 * 
 * @return map of provider:model → ProviderCosts
 */
public Map<String, ProviderCosts> getCosting() {
return new HashMap<>(costTracking);
}

/**
 * Get cost summary for a specific provider.
 * 
 * @param provider provider name
 * @return map of model → ProviderCosts
 */
public Map<String, ProviderCosts> getProviderCosts(String provider) {
Map<String, ProviderCosts> result = new HashMap<>();

for (Map.Entry<String, ProviderCosts> entry : costTracking.entrySet()) {
if (entry.getValue().provider.equals(provider)) {
result.put(entry.getValue().model, entry.getValue());
}
}

return result;
}

/**
 * Get total estimated costs across all providers.
 * 
 * @return total cost in USD
 */
public double getTotalEstimatedCost() {
return costTracking.values().stream()
.mapToDouble(c -> c.estimatedCostUSD)
.sum();
}

/**
 * Get monthly statistics for cost reporting.
 * 
 * @return summary map (totalRequests, totalInputTokens, totalOutputTokens, totalCost)
 */
public Map<String, Object> getMonthlySummary() {
long totalRequests = costTracking.values().stream()
.mapToLong(c -> c.requestCount)
.sum();

long totalInputTokens = costTracking.values().stream()
.mapToLong(c -> c.totalInputTokens)
.sum();

long totalOutputTokens = costTracking.values().stream()
.mapToLong(c -> c.totalOutputTokens)
.sum();

double totalCost = this.getTotalEstimatedCost();

Map<String, Object> summary = new HashMap<>();
summary.put("totalRequests", totalRequests);
summary.put("totalInputTokens", totalInputTokens);
summary.put("totalOutputTokens", totalOutputTokens);
summary.put("totalCost", totalCost);
summary.put("averageCost", totalRequests > 0 ? totalCost / totalRequests : 0);

return summary;
}
}
