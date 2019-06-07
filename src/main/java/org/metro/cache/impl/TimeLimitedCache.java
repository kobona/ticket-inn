package org.metro.cache.impl;

import org.metro.cache.alloc.Allocator;
import org.metro.cache.alloc.Detector;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <p> Created by pengshuolin on 2019/6/5
 */
public abstract class TimeLimitedCache<K,V extends TimeLimitedCache.TTLSpace> extends ConcurrentHashMap<K,V> {

    public static class TTLSpace extends Allocator.Space {

        private static final AtomicLongFieldUpdater<TTLSpace>
                ttl = AtomicLongFieldUpdater.newUpdater(TTLSpace.class, "TTL"),
                access = AtomicLongFieldUpdater.newUpdater(TTLSpace.class, "ACCESS");

        private volatile long TTL, ACCESS;
        private final long WRITE;

        protected TTLSpace(long address, int size, Detector detector) {
            super(address, size, detector);
            ACCESS = WRITE = System.currentTimeMillis() + 5;
        }

        long ttl() {
            return TTLSpace.ttl.get(this);
        }
        boolean updateTTL(long old, long ttl) {
            return TTLSpace.ttl.compareAndSet(this, old, ttl);
        }
        boolean isExpired(long now, long writeExpiry, long accessExpiry) {
            return (writeExpiry > 0 && (now - WRITE) > writeExpiry) ||
                   (accessExpiry > 0 && (now - access.get(this)) > accessExpiry);
        }
        void updateAccess(long now) {
            for (long time; (time = TTLSpace.access.get(this)) < now && ! TTLSpace.access.compareAndSet(this, time, now););
        }
    }

    class ComputationAware implements Function<K,V> {
        private V result;
        private final Function<? super K, ? extends V> function;
        ComputationAware(Function<? super K, ? extends V> function) {
            this.function = function;
        }
        public V apply(K k) {
            return (result = function.apply(k));
        }
        public void evict(K k, V v) {
            if (result != null && result != v) {
                onEvict(k, result);
            }
        }
    }

    class BiComputationAware implements BiFunction<K, V, V> {
        private V in, out;
        private final BiFunction<? super K, ? super V, ? extends V> function;
        BiComputationAware(BiFunction<? super K, ? super V, ? extends V> function) {
            this.function = function;
        }
        public V apply(K k, V v) {
            return (out = function.apply(k, (in = v)));
        }
        public void evict(K k, V v) {
            if (in != null && in != v) {
                onEvict(k, in);
            }
            if (out != null && out != v && (in == null || out != in)) {
                onEvict(k, out);
            }
        }
    }

    private final EvictStrategy strategy;
    private final int maximumSize;
    private final long expiryAfterAccess;
    private final long expiryAfterWrite;

    protected TimeLimitedCache(EvictStrategy strategy, int maximumSize, long expiryAfterAccess, long expiryAfterWrite) {
        this.strategy = (strategy == null) ? EvictStrategy.FIFO: strategy;
        this.maximumSize = Math.max(0, maximumSize);
        this.expiryAfterAccess = Math.max(0, expiryAfterAccess);
        this.expiryAfterWrite = Math.max(0, expiryAfterWrite);
    }

    @Override
    public V get(Object key) {
        V v = super.get(key);
        if (v != null) {
            long now = System.currentTimeMillis();
            if ((expiryAfterWrite > 0 || expiryAfterAccess > 0) &&
                v.isExpired(System.currentTimeMillis(), expiryAfterWrite, expiryAfterAccess)) {
                if (super.remove(key) == v) {
                    onEvict((K) key, v);
                }
                return null;
            }
            v.updateAccess(now);
            strategy.updateTTL(v, now);
        }
        return v;
    }

    @Override
    public V put(K key, V value) {
        V v = super.put(key, value);
        if (v != null) {
            onEvict(key, v);
        }
        return null;
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        BiComputationAware aware = new BiComputationAware(remappingFunction);
        V v = super.compute(key, aware);
        aware.evict(key, v);
        return v;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        ComputationAware aware = new ComputationAware(mappingFunction);
        V v = super.computeIfAbsent(key, aware);
        aware.evict(key, v);
        return null;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        BiComputationAware aware = new BiComputationAware(remappingFunction);
        V v = super.computeIfPresent(key, aware);
        aware.evict(key, v);
        return null;
    }

    @Override
    public V remove(Object key) {
        V v = super.remove(key);
        if (v != null) {
            onEvict((K)key, v);
        }
        return null;
    }

    @Override
    public void clear() {
        Iterator<Entry<K, V>> iter = entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K, V> entry = iter.next();
            iter.remove();
            onEvict(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    protected abstract void onEvict(K k, V v);

    protected void evictExpired() {
        doEviction(0);
    }

    private final AtomicLong lastEvictionTick = new AtomicLong();

    private void doEviction(int evictNum) {

        EntryExaminer examiner = new EntryExaminer();

        long lastEviction = lastEvictionTick.get();
        if (examiner.now - lastEviction < 100 ||
            ! lastEvictionTick.compareAndSet(lastEviction, examiner.now)) {
            return;
        }

        if (maximumSize > 0) {
            evictNum += Math.max(0, size() - maximumSize);
        }

        Iterator<Entry<K, V>> iterator = entrySet().iterator();
        if (evictNum <= 1) {
            Entry<K,V> min = null;
            while (iterator.hasNext()) {
                Entry<K, V> entry = iterator.next();
                if (examiner.isExpired(entry)) {
                    examiner.doEvict(entry); // TODO: concurrent mod
                    evictNum--;
                } else if (evictNum > 0) {
                    if (min == null || examiner.compare(entry, min) < 0) {
                        min = entry;
                    }
                }
            }
            if (evictNum > 0 && min != null) {
                examiner.doEvict(min);
            }
        } else {
            PriorityQueue<Entry<K,V>> queue = new PriorityQueue<>(evictNum, examiner);
            while (iterator.hasNext()) {
                Entry<K, V> entry = iterator.next();
                if (examiner.isExpired(entry)) {
                    examiner.doEvict(entry); // TODO: concurrent mod
                    evictNum--;
                } else if (evictNum > 0) {
                    if (queue.size() < evictNum) {
                        queue.add(entry);
                    } else if (examiner.compare(entry, queue.peek()) < 0) {
                        queue.poll();
                        queue.add(entry);
                    }
                }
            }
            while (evictNum-- > 0 && ! queue.isEmpty()) {
                examiner.doEvict(queue.poll());
            }
        }
    }

    private class EntryExaminer implements Comparator<Entry<K,V>> {
        final long now = System.currentTimeMillis();
        public int compare(Entry<K, V> o1, Entry<K, V> o2) {
            long w1 = strategy.weightTTL(o1.getValue(), now);
            long w2 = strategy.weightTTL(o2.getValue(), now);
            return - Long.compare(w1, w2); // reverse
        }
        public boolean isExpired(Entry<K, V> o) {
            return o.getValue().isExpired(now, expiryAfterWrite, expiryAfterAccess);
        }
        public void doEvict(Entry<K, V> o) {
            if (TimeLimitedCache.super.remove(o.getKey()) == o.getValue()) {
                onEvict(o.getKey(), o.getValue());
            }
        }
    }

    public enum EvictStrategy {
        FIFO {
            void updateTTL(TTLSpace space, long now) {
                space.updateTTL(0, CLOCK.incrementAndGet());
            }
            long weightTTL(TTLSpace space, long now) {
                return space.ttl();
            }
        }, LRU { // LRU-2
            void updateTTL(TTLSpace space, long now) {
                long old = space.ttl();
                long tick = CLOCK.incrementAndGet();
                long flag = (old == 0 ? 0: 1L) << 63;
                long ttl = (tick & (~0L >>> 1)) | flag;
                while ((old & (~0L >>> 1)) < tick && ! space.updateTTL(old, ttl)) {
                    old = space.ttl();
                    ttl = (tick & (~0L >>> 1)) | (1L << 63);
                }
            }
            long weightTTL(TTLSpace space, long now) {
                long ttl = space.ttl();
                long flag = ttl >>> 63;
                return flag == 0 ? 0 : ttl & (~0L >>> 1);
            }
        }, LFU { // LFU-AD
            final int TIMESTAMP_BITS = 40;
            final int FREQUENCY_BITS = 24;
            void updateTTL(TTLSpace space, long now) {
                long old = space.ttl();
                long timestamp = old >>> FREQUENCY_BITS;
                long frequency = old & (~0L >>> TIMESTAMP_BITS);

                long elapsed = TimeUnit.MILLISECONDS.toMinutes(Math.min(~0L >>> FREQUENCY_BITS, now - ERA));
                while (timestamp < elapsed) {
                    double rand = ThreadLocalRandom.current().nextDouble();
                    if (1./(frequency+1) > rand) { // decay counter
                        frequency++;
                        frequency &= (~0L >>> TIMESTAMP_BITS);
                    }
                    long ttl = elapsed << FREQUENCY_BITS | frequency & (~0L >>> TIMESTAMP_BITS);
                    if (space.updateTTL(old, ttl)) {
                        break;
                    }
                    old = space.ttl();
                    timestamp = old >>> FREQUENCY_BITS;
                    frequency = old & (~0L >>> TIMESTAMP_BITS);
                }
            }
            long weightTTL(TTLSpace space, long now) {
                long ttl = space.ttl();
                long timestamp = ttl >>> FREQUENCY_BITS;
                long frequency = ttl & (~0L >>> TIMESTAMP_BITS);
                long decay = TimeUnit.MILLISECONDS.toMinutes(Math.max(now- ERA, timestamp) - timestamp);
                return frequency - decay;
            }
        };

        private final static long ERA = System.currentTimeMillis();
        private final static AtomicLong CLOCK = new AtomicLong(0);

        abstract void updateTTL(TTLSpace space, long now);
        abstract long weightTTL(TTLSpace space, long now);
    }

}
