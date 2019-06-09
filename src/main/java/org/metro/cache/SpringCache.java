package org.metro.cache;

import org.metro.cache.impl.CacheBuilder;
import org.metro.cache.impl.SelfEvictCache;
import org.metro.cache.serial.SpaceWrapper;
import org.springframework.cache.Cache;

import java.util.Objects;
import java.util.concurrent.Callable;

public class SpringCache implements Cache {

    private SelfEvictCache<Object, SpaceWrapper> cache;

    public SpringCache(SelfEvictCache<Object, SpaceWrapper> cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return cache.name();
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
        SpaceWrapper wrapper = cache.get(o);
        if (wrapper != null && clazz.isAssignableFrom(wrapper.clazz())) {
            throw new ClassCastException(
                    wrapper.clazz().getSimpleName() + "->" + clazz.getSimpleName());
        }
        return wrapper == null? null: (T) wrapper.get();
    }

    @Override
    public <T> T get(Object o, Callable<T> callable) {
        T result;
        SpaceWrapper wrapper = cache.get(o);
        if (wrapper != null) {
            result = (T) wrapper.get();
        } else {
            try {
                SpaceWrapper space = cache.compute(o, (k, v) -> {
                    try {
                        return v != null ? v :
                                SpaceWrapper.put(callable.call(), cache);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                result = space == null? null: (T) space.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void put(Object k, Object v) {
        cache.put(k, SpaceWrapper.put(v, cache));
    }

    @Override
    public ValueWrapper putIfAbsent(Object o, Object v) {
        return cache.computeIfAbsent(o, x ->
                Objects.requireNonNull( SpaceWrapper.put(x, cache),
                        "Not enough space")
        );
    }

    @Override
    public void evict(Object o) {
        cache.remove(o);
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
