package org.metro.cache;

import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;

public class ShiroCacheManager extends AbstractCacheManager {

    @Override
    protected Cache createCache(String s) throws CacheException {
        return null;
    }

}
