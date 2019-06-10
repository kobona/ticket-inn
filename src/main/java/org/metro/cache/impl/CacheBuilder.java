package org.metro.cache.impl;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.metro.cache.alloc.Allocator;
import org.metro.cache.alloc.Memory;
import org.metro.cache.impl.TimeLimitedCache.EvictStrategy;

public class CacheBuilder {

    private static final Pattern naming =
            Pattern.compile("^([A-Za-z0-9_]+):([-.\\w]+)$");

    private static final Pattern spacing =
            Pattern.compile("^([0-9]+)(kb|mb|gb|KB|MB|GB)$");

    private final String memoryScope, cacheName;

    int maximumSize;
    long expiryAfterAccess;
    long expiryAfterWrite;
    long virtualSpace = 1 << 30;
    EvictStrategy strategy;

    public CacheBuilder(String name) {
        Matcher matcher = naming.matcher(name);
        if (! matcher.find()) {
            throw new IllegalArgumentException("Illegal cache naming");
        }
        memoryScope = matcher.group(1);
        cacheName = matcher.group(2);
    }

    public CacheBuilder virtualSpace(String virtualSpace) {
        Matcher matcher = spacing.matcher(virtualSpace);
        if (! matcher.find()) {
            throw new IllegalArgumentException("Illegal virtual spacing");
        }
        long space = Long.valueOf(matcher.group(1));
        switch (matcher.group(2).toUpperCase()) {
            case "GB": space *= 1024;
            case "MB": space *= 1024;
            case "KB": space *= 1024;
        }
        this.virtualSpace = space;
        return this;
    }

    public CacheBuilder maximumSize(int maximumSize) {
        if (maximumSize < 0)
            throw new IllegalArgumentException("maximumSize < 0");
        this.maximumSize = maximumSize;
        return this;
    }

    public CacheBuilder expiryAfterAccess(int expiryAfterAccess) {
        if (expiryAfterAccess < 0)
            throw new IllegalArgumentException("expiryAfterAccess < 0");
        this.expiryAfterAccess = expiryAfterAccess;
        return this;
    }

    public CacheBuilder expiryAfterWrite(int expiryAfterWrite) {
        if (expiryAfterWrite < 0)
            throw new IllegalArgumentException("expiryAfterAccess < 0");
        this.expiryAfterWrite = expiryAfterWrite;
        return this;
    }

    public CacheBuilder applyLRU() {
        this.strategy = EvictStrategy.LRU;
        return this;
    }

    public CacheBuilder applyLFU() {
        this.strategy = EvictStrategy.LFU;
        return this;
    }

    public CacheBuilder applyFIFO() {
        this.strategy = EvictStrategy.FIFO;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <K,V extends Allocator.Space> SelfEvictCache<K,V> build() {
        Memory memory = CacheRegistry.getMemory(memoryScope, virtualSpace);
        return CacheRegistry.getCache(memoryScope + ":" + cacheName, memory, this);
    }

    public <T> T build(Function<SelfEvictCache, T> function) {
        return function.apply(build());
    }
}