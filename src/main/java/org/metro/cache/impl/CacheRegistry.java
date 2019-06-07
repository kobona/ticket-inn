package org.metro.cache.impl;

import org.metro.cache.alloc.Memory;

import org.metro.cache.serial.SpaceWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacheRegistry {



    private static final Map<String, Memory>
            memories = new ConcurrentHashMap<>();

    private static final Map<String, SelfEvictCache>
            caches = new ConcurrentHashMap<>();

    static Memory getMemory(String name, int size) {
        return memories.computeIfAbsent(name, (k)-> {
            try {
                return new Memory(name, size, true, SpaceWrapper::new);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static SelfEvictCache getCache(String name, Memory memory, CacheBuilder builder) {
        return caches.computeIfAbsent(name, (k)->
                new SelfEvictCache<>(name, memory,
                        builder.strategy,
                        builder.maximumSize,
                        builder.expiryAfterAccess,
                        builder.expiryAfterWrite));
    }
}
