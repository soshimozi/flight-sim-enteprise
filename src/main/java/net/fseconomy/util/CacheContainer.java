package net.fseconomy.util;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CacheContainer
{
    private static final EmbeddedCacheManager CACHE_MANAGER;

    static
    {
        CACHE_MANAGER = new DefaultCacheManager();
    }

    /**
     * Retrieves the default cache.
     * @param <K> type used as keys in this cache
     * @param <V> type used as values in this cache
     * @return a cache
     */
//    public static <K, V> Cache<K, V> getCache() {
//        return CACHE_MANAGER.getCache();
//    }

    /**
     * Retrieves a named cache.
     * @param cacheName name of cache to retrieve
     * @param <K> type used as keys in this cache
     * @param <V> type used as values in this cache
     * @return a cache
     */
    public static <K, V> Cache<K, V> getCache(String cacheName) {
        if (cacheName == null) throw new NullPointerException("Cache name cannot be null!");
        return CACHE_MANAGER.getCache(cacheName);
    }

    /**
     * Retrieves the embedded cache manager.
     * @return a cache manager
     */
    public static EmbeddedCacheManager getCacheContainer() {
        return CACHE_MANAGER;
    }
}
