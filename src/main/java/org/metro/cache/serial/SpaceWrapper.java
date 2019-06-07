package org.metro.cache.serial;

import org.metro.cache.alloc.Detector;
import org.metro.cache.alloc.Memory;
import org.metro.cache.impl.TimeLimitedCache;
import org.springframework.cache.Cache;

public class SpaceWrapper extends TimeLimitedCache.TTLSpace implements Cache.ValueWrapper {

    private Class clazz;

    public SpaceWrapper(long address, int size, Detector detector) {
        super(address, size, detector);
    }

    public Object get() {
        try {
            return Serialization.read(this, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class clazz() {
        return clazz;
    }

    public static SpaceWrapper put(Object value, Memory mem) {
        try {
            SpaceWrapper warp = (SpaceWrapper) Serialization.write(value, mem);
            warp.clazz = value.getClass();
            return warp;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}