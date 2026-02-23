package systems.symbol.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LRUCacheTest {

@Test
public void testCapacityLimitsAndEviction() {
LRUCache<String, String> lruCache = new LRUCache<>(3);

lruCache.put("1", "One");
lruCache.put("2", "Two");
lruCache.put("3", "Three");

assertEquals(3, lruCache.size(), "Initial cache size should be 3");
assertNotNull(lruCache.get("1"), "Cache should contain key '1'");
assertNotNull(lruCache.get("2"), "Cache should contain key '2'");
assertNotNull(lruCache.get("3"), "Cache should contain key '3'");

lruCache.put("4", "Four");

assertEquals(3, lruCache.size(), "Cache size should remain at 3 after eviction");
assertNotNull(lruCache.get("2"), "Cache should contain key '2'");
assertNotNull(lruCache.get("3"), "Cache should contain key '3'");
assertNotNull(lruCache.get("4"), "Cache should contain key '4'");
assertNull(lruCache.get("1"), "Cache should not contain evicted key '1'");
}

@Test
public void testCacheClear() {
LRUCache<String, String> lruCache = new LRUCache<>(3);

lruCache.put("1", "One");
lruCache.put("2", "Two");
lruCache.put("3", "Three");

assertEquals(3, lruCache.size(), "Initial cache size should be 3");

lruCache.clear();

assertEquals(0, lruCache.size(), "Cache size should be 0 after clearing");
assertNull(lruCache.get("1"), "Cache should not contain key '1' after clearing");
assertNull(lruCache.get("2"), "Cache should not contain key '2' after clearing");
assertNull(lruCache.get("3"), "Cache should not contain key '3' after clearing");
}
}
