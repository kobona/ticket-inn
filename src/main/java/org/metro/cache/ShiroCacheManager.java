package org.metro.cache;

import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ShiroCacheManager extends AbstractCacheManager {

    private final ConcurrentMap<String, ShiroCache> cacheMap = new ConcurrentHashMap<>();

    @Override
    protected Cache createCache(String name) throws CacheException {
        String config = String.format("{\"name\":\"shiro:%s\"}", name);
        return cacheMap.computeIfAbsent(config, (k)->
                CacheConfig.parseJsonConfig(config).build(ShiroCache::new));
    }

}
