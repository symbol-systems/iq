package systems.symbol.platform;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LRUCacheTest {

@Test
public void testCapacityLimitsAndEviction() {
LRUCache<String, String> lruCache = new LRUCache<>(3);

lruCache.put("1", "One");
lruCache.put("2", "Two");
lruCache.put("3", "Three");

Assert.assertEquals(lruCache.size(), 3, "Initial cache size should be 3");
Assert.assertNotNull(lruCache.get("1"), "Cache should contain key '1'");
Assert.assertNotNull(lruCache.get("2"), "Cache should contain key '2'");
Assert.assertNotNull(lruCache.get("3"), "Cache should contain key '3'");
Assert.assertEquals(lruCache.size(), 3, "Cache size should remain at 3 after eviction");

lruCache.put("4", "Four");

Assert.assertEquals(lruCache.size(), 3, "Cache size should remain at 3 after eviction");
Assert.assertNotNull(lruCache.get("2"), "Cache should contain key '2'");
Assert.assertNotNull(lruCache.get("3"), "Cache should contain key '3'");
Assert.assertNotNull(lruCache.get("4"), "Cache should contain key '4'");
Assert.assertNull(lruCache.get("1"), "Cache should not contain evicted key '1'");
}

@Test
public void testCacheClear() {
LRUCache<String, String> lruCache = new LRUCache<>(3);

lruCache.put("1", "One");
lruCache.put("2", "Two");
lruCache.put("3", "Three");

Assert.assertEquals(lruCache.size(), 3, "Initial cache size should be 3");

lruCache.clear();

Assert.assertEquals(lruCache.size(), 0, "Cache size should be 0 after clearing");
Assert.assertNull(lruCache.get("1"), "Cache should not contain key '1' after clearing");
Assert.assertNull(lruCache.get("2"), "Cache should not contain key '2' after clearing");
Assert.assertNull(lruCache.get("3"), "Cache should not contain key '3' after clearing");
}
}
