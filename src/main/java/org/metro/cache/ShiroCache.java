package org.metro.cache;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.metro.cache.impl.CacheTemplate;
import org.metro.cache.serial.SpaceWrapper;

import java.util.*;

public class ShiroCache<K,V> implements Cache<K,V> {

    private CacheTemplate<K,V> cache;

    public ShiroCache(CacheTemplate<K,V> cache) {
        this.cache = cache;
    }

    @Override
    public V get(K k) throws CacheException {
        SpaceWrapper<V> wrapper = cache.getIfPresent(k);
        return wrapper == null? null: wrapper.get();
    }

    @Override
    public V put(K k, V v) throws CacheException {
        return cache.put(k, v, false);
    }

    @Override
    public V remove(K k) throws CacheException {
        return cache.remove(k, false);
    }

    @Override
    public void clear() throws CacheException {
        cache.clear();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Set<K> keys() {
        return cache.keySet();
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return cache.values();
            }
            @Override
            public int size() {
                return cache.size();
            }
        };
    }
}
