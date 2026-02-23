package systems.symbol.mcp;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;

import systems.symbol.realm.I_Realm;

/**
 * Thread-safe MCP Server implementation that manages registered adapters and a
 * global tool index. Supports multi-realm setups by allowing adapters from
 * multiple realms to provide tools; callers can prefer an adapter for a
 * particular realm when invoking tools.
 */
public class MCPService implements I_MCPRealmService {

public MCPService(I_Realm realm) {
this.realm = realm;
}

// Registered adapters
private final Set<I_MCPAdapter> adapters = new CopyOnWriteArraySet<>();

// Tool name -> set of adapters that expose that tool
private final Map<String, Set<I_MCPAdapter>> toolIndex = new ConcurrentHashMap<>();

// Optional default realm used for selecting adapters when ambiguous
private final I_Realm realm;

@Override
public org.eclipse.rdf4j.model.IRI getSelf() {
return realm == null ? null : realm.getSelf();
}

@Override
public I_Realm getRealm() {
return realm;
}

@Override
public boolean registerAdapter(I_MCPAdapter adapter) {
if (adapter == null) {
return false;
}
boolean added = adapters.add(adapter);
if (added) {
for (I_MCPToolManifest tool : adapter.listTools()) {
toolIndex.computeIfAbsent(tool.getName(), k -> ConcurrentHashMap.newKeySet()).add(adapter);
}
}
return added;
}

@Override
public boolean unregisterAdapter(I_MCPAdapter adapter) {
if (adapter == null) {
return false;
}
boolean removed = adapters.remove(adapter);
if (removed) {
for (I_MCPToolManifest tool : adapter.listTools()) {
Set<I_MCPAdapter> adaptersForTool = toolIndex.get(tool.getName());
if (adaptersForTool != null) {
adaptersForTool.remove(adapter);
if (adaptersForTool.isEmpty()) {
toolIndex.remove(tool.getName());
}
}
}
}
return removed;
}

@Override
public Collection<I_MCPAdapter> getAdapters() {
return Collections.unmodifiableSet(adapters);
}

/**
 * Returns adapters that provide the named tool.
 */
public Set<I_MCPAdapter> getAdaptersForTool(String toolName) {
Set<I_MCPAdapter> s = toolIndex.get(toolName);
if (s == null) {
return Collections.emptySet();
}
return Collections.unmodifiableSet(s);
}

@Override
public Optional<I_MCPAdapter> getAdapterForTool(String toolName) {
return getAdapterForTool(toolName, realm);
}

/**
 * Overload that prefers adapters in the provided realm when multiple adapters
 * expose the same tool.
 */
public Optional<I_MCPAdapter> getAdapterForTool(String toolName, I_Realm preferredRealm) {
Set<I_MCPAdapter> candidates = toolIndex.get(toolName);
if (candidates == null || candidates.isEmpty()) {
return Optional.empty();
}
if (preferredRealm != null) {
for (I_MCPAdapter a : candidates) {
if (preferredRealm.equals(a.getRealm())) {
return Optional.of(a);
}
}
}
// otherwise return a deterministic adapter (first by identity hash)
return candidates.stream().sorted((a, b) -> a.getSelf().stringValue().compareTo(b.getSelf().stringValue())).findFirst();
}

@Override
public Collection<I_MCPToolManifest> listAllTools() {
return adapters.stream().flatMap(a -> a.listTools().stream()).collect(Collectors.toSet());
}

@Override
public Optional<I_MCPToolManifest> getTool(String toolName) {
for (I_MCPAdapter a : adapters) {
I_MCPToolManifest t = a.getTool(toolName);
if (t != null) {
return Optional.of(t);
}
}
return Optional.empty();
}

/**
 * Attempts to resolve an adapter for the tool, preferring the configured
 * default realm when ambiguous.
 */
@Override
public I_MCPResult invokeTool(String toolName, Model input) throws Exception {
Optional<I_MCPAdapter> adapter = getAdapterForTool(toolName, realm);
if (!adapter.isPresent()) {
throw new IllegalArgumentException("Tool not found: " + toolName);
}
org.eclipse.rdf4j.model.IRI toolIRI = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
.createIRI("urn:mcp:tool:" + toolName);
return adapter.get().invoke(toolIRI, input);
}

/**
 * Overloaded invocation that explicitly selects an adapter by realm preference.
 */
public I_MCPResult invokeTool(String toolName, Model input, I_Realm preferredRealm) throws Exception {
Optional<I_MCPAdapter> adapter = getAdapterForTool(toolName, preferredRealm);
if (!adapter.isPresent()) {
throw new IllegalArgumentException("Tool not found for realm: " + toolName);
}
org.eclipse.rdf4j.model.IRI toolIRI = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance()
.createIRI("urn:mcp:tool:" + toolName);
return adapter.get().invoke(toolIRI, input);
}

/**
 * Convenience: list adapters for a realm
 */
public Set<I_MCPAdapter> getAdaptersByRealm(I_Realm realm) {
return adapters.stream().filter(a -> realm.equals(a.getRealm())).collect(Collectors.toSet());
}
}
