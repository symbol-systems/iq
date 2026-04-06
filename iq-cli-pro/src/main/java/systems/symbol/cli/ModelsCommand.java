package systems.symbol.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import systems.symbol.io.Display;
import systems.symbol.llm.gpt.LLMFactory;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * CLI Command: Manage LLM providers and models.
 * 
 * Supports listing providers, testing connectivity, showing configuration,
 * setting default providers, viewing token costs, and managing configuration.
 * 
 * Usage:
 *   iq models list   # List all LLM providers
 *   iq models list --format json|table|list # Custom output format
 *   iq models show <provider># Show provider details
 *   iq models test <provider># Test provider connectivity
 *   iq models set-default <provider> # Set default provider
 *   iq models cost [--month] # View token usage/costs
 *   iq models config set <key> <value>   # Update configuration
 *   iq models config show [<key>]# Show configuration
 * 
 * @author Symbol Systems
 */
@CommandLine.Command(name = "models", description = "LLM provider and model management")
public class ModelsCommand extends CompositeCommand {
private static final Logger log = LoggerFactory.getLogger(ModelsCommand.class);

@CommandLine.Parameters(index = "0", arity = "0..1", description = "action: list (default) | show | test | set-default | cost | config")
String action = "list";

@CommandLine.Parameters(index = "1", arity = "0..1", description = "Provider name or additional parameter")
String provider = null;

@CommandLine.Parameters(index = "2", arity = "0..1", description = "Configuration value (for config set)")
String value = null;

@CommandLine.Option(names = {"--format"}, description = "Output format: list, json, table", defaultValue = "list")
String format = "list";

@CommandLine.Option(names = {"--month"}, description = "Show monthly cost statistics")
boolean showMonth = false;

public ModelsCommand(CLIContext context, CommandLine cli) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
display("iq.models.error: IQ not initialized");
return null;
}

try {
switch(action.toLowerCase()) {
case "list":
return handleList();
case "show":
return handleShow();
case "test":
return handleTest();
case "set-default":
return handleSetDefault();
case "cost":
return handleCost();
case "config":
return handleConfig();
default:
display("iq.models.error: unknown action: " + action);
display("Available actions: list, show, test, set-default, cost, config");
return null;
}
} catch (Exception e) {
log.error("iq.models.error: {} - {}", action, e.getMessage(), e);
display("iq.models.error: " + e.getMessage());
return null;
}
}

/**
 * Handle: iq models list
 * Lists all available LLM providers and models from the realm.
 */
private Object handleList() throws Exception {
IQStore iq = context.newIQBase();
try {
List<Map<String, Object>> models = Display.models(iq, "index");

if (models == null || models.isEmpty()) {
display("iq.models: no models found");
log.info("iq.models.missing: no models in realm");
return null;
}

display("iq.models: " + models.size() + " model(s) available");
display();

if ("json".equalsIgnoreCase(format)) {
for (Map<String, Object> model : models) {
Display.display(model);
}
} else if ("table".equalsIgnoreCase(format)) {
display(String.format("%-30s | %-50s | %-20s", "Model", "ID", "Type"));
display("-".repeat(110));
for (Map<String, Object> model : models) {
Object label = model.get("label");
Object id = model.get(NS.KEY_AT_ID);
Object type = model.get("type");
displayf("%-30s | %-50s | %-20s", 
label != null ? label : "(unnamed)", 
id != null ? id : "(unknown)",
type != null ? type : "(unknown)");
}
} else {
// default list format
for (Map<String, Object> model : models) {
Object label = model.get("label");
Object id = model.get(NS.KEY_AT_ID);

if (label != null) {
display("  ✓ " + label + " @ " + id);
} else {
display("  ✓ " + model);
}

// Show additional metadata if available
Object version = model.get("version");
if (version != null) {
display("version: " + version);
}
Object prov = model.get("provider");
if (prov != null) {
display("provider: " + prov);
}
}
}

log.info("iq.models.found: " + models.size());

// Also show available LLM providers via ServiceLoader
display();
display("Available LLM providers:");
ServiceLoader.load(systems.symbol.llm.I_LLMProvider.class)
.forEach(p -> display("  ✓ " + p.scheme()));

return models;
} finally {
try {
iq.close();
} catch (Exception ignored) {}
}
}

/**
 * Handle: iq models show <provider>
 * Shows details for a specific LLM provider.
 */
private Object handleShow() throws Exception {
if (provider == null || provider.isEmpty()) {
display("iq.models.error: provider name required for 'show' action");
return null;
}

IQStore iq = context.newIQBase();
try {
List<Map<String, Object>> models = Display.models(iq, provider);

if (models == null || models.isEmpty()) {
display("iq.models.error: no models found for provider: " + provider);
return null;
}

display("Provider: " + provider);
display("Models: " + models.size());
display();

for (Map<String, Object> model : models) {
display("Name: " + model.getOrDefault("label", "(unnamed)"));
display("ID  : " + model.getOrDefault(NS.KEY_AT_ID, "(unknown)"));
display("Type: " + model.getOrDefault("type", "(unknown)"));

// Show all available metadata
for (Map.Entry<String, Object> entry : model.entrySet()) {
if (!entry.getKey().matches("label|@id|type|provider")) {
display("  " + entry.getKey() + ": " + entry.getValue());
}
}
display();
}

log.info("iq.models.show: {} - {} provider(s)", provider, models.size());
return models;
} finally {
try {
iq.close();
} catch (Exception ignored) {}
}
}

/**
 * Handle: iq models test <provider>
 * Tests connectivity to a specific LLM provider.
 */
private Object handleTest() throws Exception {
if (provider == null || provider.isEmpty()) {
display("iq.models.error: provider name required for 'test' action");
return null;
}

try {
// Discover provider via ServiceLoader
boolean found = false;
for (systems.symbol.llm.I_LLMProvider llmProvider : 
 ServiceLoader.load(systems.symbol.llm.I_LLMProvider.class)) {
if (provider.equalsIgnoreCase(llmProvider.scheme())) {
found = true;
display("Testing LLM provider: " + provider);
display("  Scheme: " + llmProvider.scheme());
display("  Status: ✓ Provider available and registered");
log.info("iq.models.test: {} - available", provider);
return "provider:available:" + provider;
}
}

if (!found) {
display("iq.models.error: provider not found: " + provider);
display("Available providers: openai, groq");
log.warn("iq.models.test: {} - not found", provider);
return null;
}
} catch (Exception e) {
display("iq.models.error: failed to test provider: " + e.getMessage());
log.error("iq.models.test.error: {}", e.getMessage(), e);
return null;
}

return null;
}

/**
 * Handle: iq models set-default <provider>
 * Sets the default LLM provider for the realm.
 */
private Object handleSetDefault() throws Exception {
if (provider == null || provider.isEmpty()) {
display("iq.models.error: provider name required for 'set-default' action");
return null;
}

try {
display("Setting default LLM provider: " + provider);

// Create config manager and persist setting
systems.symbol.llm.config.LLMConfigManager configManager = 
new systems.symbol.llm.config.LLMConfigManager(context.getKernelContext());

// Validate provider exists
boolean found = false;
for (systems.symbol.llm.I_LLMProvider llmProvider : 
 ServiceLoader.load(systems.symbol.llm.I_LLMProvider.class)) {
if (provider.equalsIgnoreCase(llmProvider.scheme())) {
found = true;
break;
}
}

if (!found) {
display("iq.models.error: provider not found: " + provider);
return null;
}

// Save default provider
configManager.setDefaultProvider(provider);
display("  Configuration saved to: .iq/llm-config.yaml");
display("  ✓ Default provider set to: " + provider);

log.info("iq.models.set-default: {}", provider);
return "default:set:" + provider;
} catch (Exception e) {
display("iq.models.error: failed to set default provider: " + e.getMessage());
log.error("iq.models.set-default.error: {}", e.getMessage(), e);
return null;
}
}

/**
 * Handle: iq models cost [--month]
 * Views token usage and costs for LLM models.
 */
private Object handleCost() throws Exception {
try {
display("LLM Token Usage & Costs");
display("-".repeat(60));

// Load cost tracking from config manager
systems.symbol.llm.config.LLMConfigManager configManager = 
new systems.symbol.llm.config.LLMConfigManager(context.getKernelContext());

Map<String, systems.symbol.llm.config.LLMConfigManager.ProviderCosts> allCosts = 
configManager.getCosting();

if (showMonth) {
display();
display("Monthly Statistics:");
Map<String, Object> summary = configManager.getMonthlySummary();

display("  Total requests: " + summary.get("totalRequests"));
display("  Total input tokens: " + summary.get("totalInputTokens"));
display("  Total output tokens: " + summary.get("totalOutputTokens"));
display("  Total cost: $" + String.format("%.2f", summary.get("totalCost")));
display("  Average per req   : $" + String.format("%.4f", summary.get("averageCost")));
}

// Show per-provider breakdown
if (!allCosts.isEmpty()) {
display();
display("Cost by Provider:");
display(String.format("  %-15s | %-20s | %10s | %10s | %10s", 
"Provider", "Model", "Requests", "Tokens", "Cost (USD)"));
display("  " + "-".repeat(70));

for (Map.Entry<String, systems.symbol.llm.config.LLMConfigManager.ProviderCosts> entry : allCosts.entrySet()) {
systems.symbol.llm.config.LLMConfigManager.ProviderCosts costs = entry.getValue();
displayf("  %-15s | %-20s | %10d | %10d | $%9.2f",
costs.provider,
costs.model,
costs.requestCount,
costs.totalInputTokens + costs.totalOutputTokens,
costs.estimatedCostUSD);
}
} else {
display();
display("  (no usage data yet - cost tracking enabled)");
}

log.info("iq.models.cost: month={}, providers={}", showMonth, allCosts.size());
return "cost:report:generated";
} catch (Exception e) {
display("iq.models.error: failed to retrieve costs: " + e.getMessage());
log.error("iq.models.cost.error: {}", e.getMessage(), e);
return null;
}
}

/**
 * Handle: iq models config <set|show> [key] [value]
 * Manages LLM configuration (requires parsing provider/key/value).
 */
private Object handleConfig() throws Exception {
// For 'config' action, provider contains the subaction (set or show)
// and value would contain the key
String subaction = provider != null ? provider.toLowerCase() : "show";

systems.symbol.llm.config.LLMConfigManager configManager = 
new systems.symbol.llm.config.LLMConfigManager(context.getKernelContext());

if ("set".equalsIgnoreCase(subaction)) {
if (value == null || value.isEmpty()) {
display("iq.models.error: config key and value required for 'config set'");
display("Usage: iq models config set PROVIDER KEY VALUE");
return null;
}

// Parse "PROVIDER:KEY=VALUE" format from value parameter
if (!value.contains(":") || !value.contains("=")) {
display("iq.models.error: invalid format, expected: PROVIDER:KEY=VALUE");
return null;
}

String[] parts = value.split(":", 2);
String providerName = parts[0];
String[] keyValue = parts[1].split("=", 2);

if (keyValue.length != 2) {
display("iq.models.error: invalid format, expected: PROVIDER:KEY=VALUE");
return null;
}

String key = keyValue[0];
String val = keyValue[1];

// Set the configuration
configManager.setProviderConfig(providerName, key, val);
display("Configuration set:");
display("  Provider: " + providerName);
display("  Key : " + key);
display("  Value   : " + val);
log.info("iq.models.config.set: {}:{} = {}", providerName, key, val);
return "config:set:" + value;
} else if ("show".equalsIgnoreCase(subaction)) {
display("Current LLM Configuration:");
display("  Default provider : " + configManager.getDefaultProvider());
display("  Config file  : .iq/llm-config.yaml");
display();

// Show all provider configurations
display("Provider Configurations:");
for (systems.symbol.llm.I_LLMProvider llmProvider : 
 ServiceLoader.load(systems.symbol.llm.I_LLMProvider.class)) {
Map<String, Object> providerConfig = configManager.getProviderConfig(llmProvider.scheme());

if (!providerConfig.isEmpty()) {
display("  " + llmProvider.scheme() + ":");
for (Map.Entry<String, Object> entry : providerConfig.entrySet()) {
display("" + entry.getKey() + ": " + entry.getValue());
}
} else {
display("  " + llmProvider.scheme() + ": (default configuration)");
}
}

log.info("iq.models.config.show");
return "config:show";
} else {
display("iq.models.error: unknown config action: " + subaction);
display("Available actions: set, show");
return null;
}
}

}
