package org.metro.cache;

import org.metro.cache.alloc.Memory;
import org.metro.cache.serial.Serialization;
import org.metro.cache.alloc.Allocator;
import org.springframework.cache.Cache;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MetroCache implements Cache {

    static class MetroValueWrapper implements ValueWrapper {
        final Class clazz;
        final Allocator.Space space;
        MetroValueWrapper(Object value, Memory mem) throws Exception {
            this.clazz = value.getClass();
            try {
                this.space = Serialization.write(value, clazz, mem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public Object get() {
            try {
                return Serialization.read(space, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final String name;
    private final Memory memory;
    private final ConcurrentMap<Object, MetroValueWrapper> cacheMap;

    public MetroCache(String name)  {
        this(name,2^30);
    }

    public MetroCache(String name, int size) {
        this.name = name;
        this.cacheMap = new ConcurrentHashMap<>();
        try {
            this.memory = new Memory("MetroCache-"+name, size, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        MetroValueWrapper wrapper = cacheMap.get(o);
        if (wrapper != null && clazz.isAssignableFrom(wrapper.clazz)) {
            throw new ClassCastException(
                    wrapper.clazz.getSimpleName() + "->" + clazz.getSimpleName());
        }
        return wrapper == null? null: (T) wrapper.get();
    }

    @Override
    public <T> T get(Object o, Callable<T> callable) {
        T result;
        MetroValueWrapper wrapper = cacheMap.get(o);
        if (wrapper != null) {
            result = (T) wrapper.get();
        } else {
            try {
                if ((result = callable.call()) != null) {
                    wrapper = new MetroValueWrapper(result, memory);
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
        try {
            cacheMap.put(k, new MetroValueWrapper(v, memory));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object o, Object v) {
        cacheMap.computeIfAbsent(o, x -> {
            try {
                return new MetroValueWrapper(x, memory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return null;
    }

    @Override
    public void evict(Object o) {
        MetroValueWrapper wrapper = cacheMap.remove(o);
        if (wrapper != null) {
            memory.release(wrapper.space);
        }
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<Object, MetroValueWrapper>> iter = cacheMap.entrySet().iterator();
        while (iter.hasNext()) {
            memory.release(iter.next().getValue().space);
            iter.remove();
        }
    }
}
