package systems.symbol.platform;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> {
private final int capacity;
private final LinkedHashMap<K, V> cache;

public LRUCache(int capacity) {
this.capacity = capacity;
this.cache = new LinkedHashMap<>(capacity, 0.75f, true);
}

public void put(K key, V value) {
cache.put(key, value);
optimize();
}

public V get(K key) {
return cache.get(key);
}

public void remove(K key) {
cache.remove(key);
}

public void clear() {
cache.clear();
}

public int size() {
return cache.size();
}

public void optimize() {
if (cache.size() > capacity) {
// Determine the least recently used entry and remove it
Map.Entry<K, V> eldestEntry = cache.entrySet().iterator().next();
K eldestKey = eldestEntry.getKey();
cache.remove(eldestKey);

// Optionally, you can perform additional actions when an entry is evicted
// For example: log eviction, notify listeners, etc.
System.out.println("LRU Eviction: " + eldestKey);
}
}
}
