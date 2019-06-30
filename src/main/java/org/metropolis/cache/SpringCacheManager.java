package org.metropolis.cache;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SpringCacheManager implements CacheManager,
        InitializingBean, DisposableBean {

    private ConcurrentMap<String, SpringCache> cacheMap;
    private final String cachePrefix;

    public SpringCacheManager(String cachePrefix) {
        this.cachePrefix = cachePrefix;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cacheMap = new ConcurrentHashMap<>();
    }

    @Override
    public Cache getCache(String config) {
        return cacheMap.computeIfAbsent(config, (k)->
                CacheConfig.parseJsonConfig(config).build(cachePrefix, SpringCache::new));
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    @Override
    public void destroy() throws Exception {
        Iterator<Map.Entry<String, SpringCache>> iter = cacheMap.entrySet().iterator();
        cacheMap = null;
        while (iter.hasNext()) {
            SpringCache cache = iter.next().getValue();
            iter.remove();
            cache.clear();
        }
    }
}
