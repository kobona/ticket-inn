package org.metro.cache;

import org.metro.cache.alloc.Memory;
import org.metro.cache.impl.SelfEvictCache;
import org.metro.cache.impl.TimeLimitedCache;
import org.metro.cache.serial.SpaceWrapper;
import org.springframework.cache.Cache;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;

public class SpringCache implements Cache {

    private final String name;
    private final Memory memory;
    private final SelfEvictCache<Object, SpaceWrapper> cacheMap;

    public SpringCache(String name)  {
        this(name,2^30);
    }

    public SpringCache(String name, int size) {
        this.name = name;
        try {
            this.memory = new Memory("SpringCache-"+name, size, true, SpaceWrapper::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.cacheMap = new SelfEvictCache<>(name, memory, TimeLimitedCache.EvictStrategy.FIFO, 0, 0, 0);
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
        return cacheMap.get(o);
    }

    @Override
    public <T> T get(Object o, Class<T> clazz) {
        SpaceWrapper wrapper = cacheMap.get(o);
        if (wrapper != null && clazz.isAssignableFrom(wrapper.clazz())) {
            throw new ClassCastException(
                    wrapper.clazz().getSimpleName() + "->" + clazz.getSimpleName());
        }
        return wrapper == null? null: (T) wrapper.get();
    }

    @Override
    public <T> T get(Object o, Callable<T> callable) {
        T result;
        SpaceWrapper wrapper = cacheMap.get(o);
        if (wrapper != null) {
            result = (T) wrapper.get();
        } else {
            try {
                if ((result = callable.call()) != null) {
                    wrapper = SpaceWrapper.put(result, memory);
                    cacheMap.put(o, wrapper);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void put(Object k, Object v) {
        cacheMap.put(k, SpaceWrapper.put(v, memory));
    }

    @Override
    public ValueWrapper putIfAbsent(Object o, Object v) {
        return cacheMap.computeIfAbsent(o, x ->
                Objects.requireNonNull( SpaceWrapper.put(x, memory),
                        "Not enough space")
        );
    }

    @Override
    public void evict(Object o) {
        cacheMap.remove(o);
    }

    @Override
    public void clear() {
        cacheMap.clear();
    }
}
