package systems.symbol.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> {
private static final Logger log = LoggerFactory.getLogger(LRUCache.class);
private final int capacity;
private final Map<K, V> cache;

public LRUCache(int capacity) {
this.capacity = capacity;
this.cache = Collections.synchronizedMap(new LinkedHashMap<>(capacity, 0.75f, true));
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
synchronized (cache) {
if (cache.size() > capacity) {
Map.Entry<K, V> eldestEntry = cache.entrySet().iterator().next();
K eldestKey = eldestEntry.getKey();
cache.remove(eldestKey);
log.debug("lru.eviction: {}", eldestKey);
}
}
}
}
