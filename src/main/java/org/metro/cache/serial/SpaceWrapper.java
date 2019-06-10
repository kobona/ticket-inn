package org.metro.cache.serial;

import org.metro.cache.alloc.Detector;
import org.metro.cache.impl.SelfEvictCache;
import org.metro.cache.impl.TimeLimitedCache;
import org.springframework.cache.Cache;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class SpaceWrapper<T> extends TimeLimitedCache.TTLSpace implements Cache.ValueWrapper {

    protected Class<T> clazz;
    protected int padding;

    public SpaceWrapper(long address, int size, Detector detector) {
        super(address, size, detector);
    }

    public Class<T> clazz() {
        return clazz;
    }

    public long size() {
        return length() - padding;
    }

    public T get() {
        try {
            return Serialization.read(this, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> SpaceWrapper<T> put(Object value, SelfEvictCache cache) {
        try {
            return Objects.requireNonNull(Serialization.write(value, cache.mem()),
                    "No enough space");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}