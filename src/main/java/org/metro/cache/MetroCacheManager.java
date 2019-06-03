package org.metro.cache;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MetroCacheManager implements CacheManager,
        InitializingBean, DisposableBean {

    private ConcurrentMap<String, MetroCache> cacheMap;

    @Override
    public void afterPropertiesSet() throws Exception {
        cacheMap = new ConcurrentHashMap<>();
    }

    @Override
    public Cache getCache(String s) {
        MetroCache cache = cacheMap.get(s);
        if (cache == null) {
            cache = cacheMap.computeIfAbsent(s, x -> new MetroCache(s));
        }
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    @Override
    public void destroy() throws Exception {
        Iterator<Map.Entry<String, MetroCache>> iter = cacheMap.entrySet().iterator();
        cacheMap = null;
        while (iter.hasNext()) {
            iter.next().getValue().clear();
            iter.remove();
        }
    }
}
