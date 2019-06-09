package org.metro.cache;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.metro.cache.impl.CacheBuilder;
import org.metro.cache.impl.SelfEvictCache;
import org.metro.cache.serial.SpaceWrapper;

import java.util.*;

public class ShiroCache<K,V> implements Cache<K,V> {

    private SelfEvictCache<K,SpaceWrapper<V>> cache;

    public ShiroCache(SelfEvictCache<K,SpaceWrapper<V>> cache) {
        this.cache = cache;
    }

    @Override
    public V get(K k) throws CacheException {
        SpaceWrapper<V> wrapper = cache.get(k);
        return wrapper == null? null: wrapper.get();
    }

    @Override
    public V put(K k, V v) throws CacheException {
        cache.put(k, SpaceWrapper.put(v, cache));
        return null;
    }

    @Override
    public V remove(K k) throws CacheException {
        cache.remove(k);
        return null;
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
        Collection<SpaceWrapper<V>> values = cache.values();
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                Iterator<SpaceWrapper<V>> iterator = values.iterator();
                return new Iterator<V>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }
                    @Override
                    public V next() {
                        SpaceWrapper<V> wrapper = iterator.next();
                        return wrapper == null ? null: wrapper.get();
                    }
                };
            }
            @Override
            public int size() {
                return values.size();
            }
        };
    }
}
