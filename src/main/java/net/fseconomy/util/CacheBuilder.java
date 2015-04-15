package net.fseconomy.util;

import java.io.IOException;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author noconnor@redhat.com
 */
public class CacheBuilder
{
    private EmbeddedCacheManager cache_manager;

    public CacheBuilder(String inConfigFile) throws IOException
    {
//        if ((inConfigFile==null)||(inConfigFile.isEmpty()))
//            throw new RuntimeException(
//                    "Infinispan configuration file not found-->" + inConfigFile);
//
//        System.out.println("CacheBuilder called with " + inConfigFile);
//        cache_manager = new DefaultCacheManager(inConfigFile, true);
        cache_manager = new DefaultCacheManager();

        //add caches here programmatically
        cache_manager.defineConfiguration("serviceKeys", new ConfigurationBuilder()
                .eviction().strategy(EvictionStrategy.LRU).maxEntries(50)
                .expiration().maxIdle(60*60*1000L)
                .build());

        cache_manager.defineConfiguration("tokenCache", new ConfigurationBuilder()
                .eviction().strategy(EvictionStrategy.LRU).maxEntries(50)
                .expiration().maxIdle(60 * 60 * 1000L)
                .build());
    }

    public EmbeddedCacheManager getCacheManager() {
        return this.cache_manager;
    }
}



