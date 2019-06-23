package org.metro.cache;

import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ShiroCacheManager extends AbstractCacheManager {

    private final ConcurrentMap<String, ShiroCache> cacheMap = new ConcurrentHashMap<>();
    private final String cachePrefix;

    public ShiroCacheManager(String cachePrefix) {
        this.cachePrefix = cachePrefix;
    }

    @Override
    protected Cache createCache(String name) throws CacheException {
        String config = String.format("{\"name\":\"shiro:%s\"}", name);
        return cacheMap.computeIfAbsent(config, (k)->
                CacheConfig.parseJsonConfig(config).build(cachePrefix, ShiroCache::new));
    }

}
