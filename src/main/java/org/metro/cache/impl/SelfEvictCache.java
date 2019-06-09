package org.metro.cache.impl;

import org.metro.cache.alloc.Allocator;
import org.metro.cache.alloc.Memory;

import java.util.concurrent.atomic.LongAdder;

/**
 * <p> Created by pengshuolin on 2019/6/6
 */
public class SelfEvictCache<K,V extends Allocator.Space> extends TimeLimitedCache<K,V> {

    private final String name;
    private final Memory memory;

    public SelfEvictCache(String name, Memory memory, EvictStrategy strategy,
                          int maximumSize, long expiryAfterAccess, long expiryAfterWrite) {
        super(strategy, maximumSize, expiryAfterAccess, expiryAfterWrite);
        this.name = name;
        this.memory = memory;
    }

    private final LongAdder
            hits = new LongAdder(),
            misses  = new LongAdder(),
            eviction = new LongAdder();

    public class CacheStats {

        private final long
            hitCount = hits.sum(),
            missCount = misses.sum(),
            evictionCount = eviction.sum();

        public long hitCount() {
            return hitCount;
        }
        public long missCount() {
            return missCount;
        }
        public long evictionCount() {
            return evictionCount;
        }
        public long requestCount() {
            return hitCount + missCount;
        }
        public double hitRate() {
            return hitCount / Math.max(requestCount(), 1.0);
        }
        public double missRate() {
            return missCount /  Math.max(requestCount(), 1.0);
        }

        @Override
        public String toString() {
            new StringBuilder("CacheStats[").
                    append("name:").append(name).append(", ").
                    append("hit:").append(hitCount).append(", ").
                    append("miss:").append(missCount).append(", ").
                    append("request:").append(requestCount()).append(",").
                    append("hitRate:").append(String.format("%.2f%%", hitRate() * 100)).append(", ").
                    append("missRate:").append(String.format("%.2f%%", missRate() * 100)).append(", ").
                    append("eviction:").append(evictionCount).append("]");
            return super.toString();
        }
    }

    public Memory mem() {
        return memory;
    }

    public String name() {
        return name;
    }

    public CacheStats stats() {
        return new CacheStats();
    }

    @Override
    protected void onEvict(K k, V v) {
        memory.release(v);
        eviction.increment();
    }

    @Override
    public V get(Object key) {
        V v =  super.get(key);
        (v != null ? hits : misses).increment();
        return v;
    }
}
