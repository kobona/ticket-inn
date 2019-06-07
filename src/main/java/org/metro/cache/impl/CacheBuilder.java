package org.metro.cache.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.metro.cache.impl.TimeLimitedCache.EvictStrategy;

public class CacheBuilder {

    private static final Pattern naming =
            Pattern.compile("^([A-Za-z0-9_]+):([A-Za-z0-9_]+)$");

    final String memoryScope, cacheName;
    int maximumSize;
    long expiryAfterAccess;
    long expiryAfterWrite;
    EvictStrategy strategy;

    public CacheBuilder(String name) {
        Matcher matcher = naming.matcher(name);
        if (! matcher.find()) {
            throw new IllegalArgumentException("Illegal cache naming");
        }
        memoryScope = matcher.group(1);
        cacheName = matcher.group(2);
    }
    public CacheBuilder maximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
        return this;
    }
    public CacheBuilder expiryAfterAccess(int expiryAfterAccess) {
        this.expiryAfterAccess = expiryAfterAccess;
        return this;
    }
    public CacheBuilder expiryAfterWrite(int expiryAfterWrite) {
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
    public SelfEvictCache build() {
        return null;
    }
}