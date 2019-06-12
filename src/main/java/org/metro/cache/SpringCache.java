package org.metro.cache;

import org.metro.cache.impl.CacheTemplate;
import org.metro.cache.serial.SpaceWrapper;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class SpringCache implements Cache {

    private String name;
    private CacheTemplate cache;

    public SpringCache(CacheTemplate cache, String name) {
        this.cache = cache;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueWrapper get(Object o) {
        return cache.get(o);
    }

    @Override
    public <T> T get(Object o, Class<T> clazz) {
        SpaceWrapper wrapper = cache.getIfPresent(o);
        if (wrapper != null && clazz.isAssignableFrom(wrapper.clazz())) {
            throw new ClassCastException(
                    wrapper.clazz().getSimpleName() + "->" + clazz.getSimpleName());
        }
        return wrapper == null? null: (T) wrapper.get();
    }

    @Override
    public <T> T get(Object o, Callable<T> callable) {
        return cache.loadIfAbsent(o, k -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void put(Object k, Object v) {
        cache.put(k, v, false);
    }

    @Override
    public ValueWrapper putIfAbsent(Object o, Object v) {
        return cache.putIfAbsent(o, v, false);
    }

    @Override
    public void evict(Object o) {
        cache.remove(o, false);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
