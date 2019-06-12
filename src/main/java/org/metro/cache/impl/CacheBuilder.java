package org.metro.cache.impl;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.metro.cache.alloc.Memory;
import org.metro.cache.impl.CacheStruct.EvictStrategy;
import org.metro.cache.impl.CacheStruct.WeighStrategy;

public class CacheBuilder {

    private static final Pattern naming =
            Pattern.compile("^([A-Za-z0-9_]+):([-.\\w]+)$");

    private static final Pattern spacing =
            Pattern.compile("^([0-9]+)(kb|mb|gb|KB|MB|GB)?$");

    private final String memoryScope, cacheName;

    long maximumSize;
    long expiryAfterAccess;
    long expiryAfterWrite;
    long virtualSpace = 1 << 30;
    EvictStrategy evicting;
    WeighStrategy weighing;
    int initialCapacity = 16;
    int concurrencyLevel = 5;

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
            throw new IllegalArgumentException("Illegal spacing");
        }
        long space = Long.valueOf(matcher.group(1));
        String unit = StringUtils.defaultString(matcher.group(2));
        switch (unit.toUpperCase()) {
            case "GB": space *= 1024;
            case "MB": space *= 1024;
            case "KB": space *= 1024;
        }
        this.virtualSpace = space;
        return this;
    }

    public CacheBuilder initialCapacity(int initialCapacity) {
        if (initialCapacity <= 0)
            throw new IllegalArgumentException("initialCapacity <= 0");
        this.initialCapacity = initialCapacity;
        return this;
    }

    public CacheBuilder concurrencyLevel(int concurrencyLevel) {
        if (concurrencyLevel <= 0)
            throw new IllegalArgumentException("concurrencyLevel <= 0");
        this.concurrencyLevel = concurrencyLevel;
        return this;
    }

    public CacheBuilder maximumSize(int maximumSize) {
        if (maximumSize < 0)
            throw new IllegalArgumentException("maximumSize < 0");
        this.maximumSize = maximumSize;
        this.weighing = WeighStrategy.COUNT;
        return this;
    }

    public CacheBuilder maximumSpace(String maximumSpace) {
        Matcher matcher = spacing.matcher(maximumSpace);
        if (! matcher.find()) {
            throw new IllegalArgumentException("Illegal spacing");
        }
        long space = Long.valueOf(matcher.group(1));
        String unit = StringUtils.defaultString(matcher.group(2));
        switch (unit.toUpperCase()) {
            case "GB": space *= 1024;
            case "MB": space *= 1024;
            case "KB": space *= 1024;
        }
        this.maximumSize = space;
        this.weighing = WeighStrategy.SPACE;
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
        this.evicting = EvictStrategy.LRU;
        return this;
    }

    public CacheBuilder applyLFU() {
        this.evicting = EvictStrategy.LFU;
        return this;
    }

    public CacheBuilder applyFIFO() {
        this.evicting = EvictStrategy.FIFO;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <K,V> CacheTemplate<K,V> build() {
        Memory memory = CacheRegistry.getMemory(memoryScope, virtualSpace);
        return CacheRegistry.getCache(memoryScope + ":" + cacheName, memory, this);
    }

    public <T> T build(Function<CacheTemplate, T> function) {
        return function.apply(build());
    }

    public <T> T build(BiFunction<CacheTemplate, String, T> function) {
        return function.apply(build(), memoryScope + ":" + cacheName);
    }
}