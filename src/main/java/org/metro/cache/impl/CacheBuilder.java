package org.metro.cache.impl;

import java.lang.management.ManagementFactory;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.metro.cache.alloc.Memory;
import org.metro.cache.impl.Caching.EvictStrategy;
import org.metro.cache.impl.Caching.WeighStrategy;

public class CacheBuilder {

    private static final Pattern naming =
            Pattern.compile("^([A-Za-z0-9_]+):([-.\\w]+)$");

    private static final Pattern spacing =
            Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)(kb|mb|gb|KB|MB|GB)?$");

    private static final long systemMemory = ((OperatingSystemMXBean)
            ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();

    private final String memoryScope, cacheName;

    long maximumSize;
    long expiryAfterAccess;
    long expiryAfterWrite;
    long virtualSpace = 1L << 31;
    EvictStrategy evicting = EvictStrategy.FIFO;
    WeighStrategy weighing = WeighStrategy.COUNT;
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
        double space = Double.valueOf(matcher.group(1));
        String unit = StringUtils.defaultString(matcher.group(2));
        switch (unit.toUpperCase()) {
            case "GB": space *= 1024;
            case "MB": space *= 1024;
            case "KB": space *= 1024;
        }
        this.virtualSpace = Math.min(Math.round(space), systemMemory);
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
        double space = Double.valueOf(matcher.group(1));
        String unit = StringUtils.defaultString(matcher.group(2));
        switch (unit.toUpperCase()) {
            case "GB": space *= 1024;
            case "MB": space *= 1024;
            case "KB": space *= 1024;
        }
        this.maximumSize = Math.round(space);
        this.weighing = WeighStrategy.SPACE;
        return this;
    }

    public CacheBuilder expiryAfterAccess(int expiryAfterAccess) {
        if (expiryAfterAccess < Caching.TICK)
            throw new IllegalArgumentException("expiryAfterAccess < " + Caching.TICK);
        this.expiryAfterAccess = expiryAfterAccess;
        return this;
    }

    public CacheBuilder expiryAfterWrite(int expiryAfterWrite) {
        if (expiryAfterWrite < Caching.TICK)
            throw new IllegalArgumentException("expiryAfterAccess < " + Caching.TICK);
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
    public <K,V> CacheEngine<K,V> build(String... prefix) {
        Validate.validState(prefix == null || prefix.length <= 1);
        Memory memory = CacheRegistry.getMemory(ArrayUtils.isEmpty(prefix) ? null: prefix[0], memoryScope, virtualSpace);
        return CacheRegistry.getCache(memoryScope + ":" + cacheName, memory, this);
    }

    public <T> T build(String prefix, Function<CacheEngine, T> function) {
        return function.apply(build(prefix));
    }

    public <T> T build(String prefix, BiFunction<CacheEngine, String, T> function) {
        return function.apply(build(prefix), memoryScope + ":" + cacheName);
    }
}