package org.metro.cache.impl;

import org.metro.cache.alloc.Memory;
import org.metro.cache.serial.SpaceWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class CacheRegistry {

    private static final Map<String, Memory>
            memories = new ConcurrentHashMap<>();

    private static final Map<String, SelfEvictCache>
            caches = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService cacheCleaner =
            Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("CacheCleaner");
                    return t;
                }
            });

    static {
//        cacheCleaner.scheduleWithFixedDelay(new Runnable() {
//            public void run() {
//                for (SelfEvictCache cache : caches.values()) {
//                    try {
//                        cache.evictExpired();
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                    }
//                }
//            }
//        }, 1, 1, TimeUnit.SECONDS);
    }

    static Memory getMemory(String name, int size) {
        return memories.computeIfAbsent(name, (k)-> {
            try {
                return new Memory(name, size, true, SpaceWrapper::new);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static synchronized SelfEvictCache getCache(String name, Memory memory, CacheBuilder builder) {
        if (caches.containsKey(name)) {
            throw new IllegalArgumentException("Duplicated cache name:" + name);
        }
        return caches.computeIfAbsent(name, (k)->
                new SelfEvictCache<>(name, memory,
                        builder.strategy,
                        builder.maximumSize,
                        builder.expiryAfterAccess,
                        builder.expiryAfterWrite));
    }
}
