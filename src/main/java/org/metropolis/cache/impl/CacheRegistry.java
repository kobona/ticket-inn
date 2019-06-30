package org.metropolis.cache.impl;

import org.metropolis.cache.alloc.Memory;
import org.metropolis.cache.serial.SpaceWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class CacheRegistry {

    private static final Map<String, Memory>
            memories = new ConcurrentHashMap<>();

    private static final Map<String, CacheEngine>
            caches = new ConcurrentHashMap<>();

    static Memory getMemory(String prefix, String name, long space) {
        return memories.computeIfAbsent(name, (k)-> {
            try {
                String separator = (prefix == null || prefix.isEmpty() || prefix.endsWith(File.separator) || name.startsWith(File.separator))? "": File.separator;
                return new Memory(prefix + separator + name, space, true, SpaceWrapper::new);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static synchronized CacheEngine getCache(String name, Memory memory, CacheBuilder builder) {
        if (caches.containsKey(name)) {
            throw new IllegalArgumentException("Duplicated cache name:" + name);
        }
        return caches.computeIfAbsent(name, (k)->
                new CacheEngine<>(builder, memory));
    }
}
