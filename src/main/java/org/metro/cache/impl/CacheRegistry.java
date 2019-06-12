package org.metro.cache.impl;

import org.metro.cache.alloc.Memory;
import org.metro.cache.serial.SpaceWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class CacheRegistry {

    private static final Map<String, Memory>
            memories = new ConcurrentHashMap<>();

    private static final Map<String, CacheTemplate>
            caches = new ConcurrentHashMap<>();

    static Memory getMemory(String name, long space) {
        return memories.computeIfAbsent(name, (k)-> {
            try {
                return new Memory(name, space, true, SpaceWrapper::new);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static synchronized CacheTemplate getCache(String name, Memory memory, CacheBuilder builder) {
        if (caches.containsKey(name)) {
            throw new IllegalArgumentException("Duplicated cache name:" + name);
        }
        return caches.computeIfAbsent(name, (k)->
                new CacheTemplate<>(builder, memory));
    }
}
