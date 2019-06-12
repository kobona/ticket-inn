package org.metro.cache.serial;

import org.metro.cache.alloc.Allocator;
import org.metro.cache.alloc.Detector;
import org.metro.cache.alloc.Memory;
import org.springframework.cache.Cache;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class SpaceWrapper<T> extends Allocator.Space implements Cache.ValueWrapper {

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

    public static <T> SpaceWrapper<T> put(Object value, Memory memory) {
        try {
            return Objects.requireNonNull(Serialization.write(value, memory),
                    "No enough space");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SpaceWrapper) {
            SpaceWrapper sw = (SpaceWrapper) obj;
            return  sw.clazz == clazz &&
                    sw.address() == address() &&
                    sw.length() == length();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, address(), length());
    }
}