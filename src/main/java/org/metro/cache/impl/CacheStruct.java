package org.metro.cache.impl;

import org.metro.cache.serial.SpaceWrapper;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.LongAdder;

/**
 * <p> Created by pengshuolin on 2019/6/12
 */
public class CacheStruct {

    protected final static int TICK = 10;
    protected final static long ERA = System.currentTimeMillis();
    protected static int elapsed() {
        return (int) (System.currentTimeMillis() - ERA) / TICK;
    }

    protected final AtomicLong CLOCK = new AtomicLong(0);

    protected final LongAdder
            hits = new LongAdder(),
            misses  = new LongAdder(),
            eviction = new LongAdder();

    public CacheStats stats() {
        return new CacheStats();
    }

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
            return new StringBuilder("CacheStats[").
                    append("hit:").append(hitCount).append(", ").
                    append("miss:").append(missCount).append(", ").
                    append("request:").append(requestCount()).append(",").
                    append("hitRate:").append(String.format("%.2f%%", hitRate() * 100)).append(", ").
                    append("missRate:").append(String.format("%.2f%%", missRate() * 100)).append(", ").
                    append("eviction:").append(evictionCount).append("]").toString();
        }
    }

    static class Node<K,V> implements Map.Entry<K,SpaceWrapper<V>> {

        private static final AtomicLongFieldUpdater<Node>
                ttl = AtomicLongFieldUpdater.newUpdater(Node.class, "TTL");
        private static final AtomicIntegerFieldUpdater<Node>
                write = AtomicIntegerFieldUpdater.newUpdater(Node.class, "WRITE"),
                access = AtomicIntegerFieldUpdater.newUpdater(Node.class, "ACCESS");

        private volatile long TTL;
        private volatile int WRITE, ACCESS;

        private final K key;
        private final int hash;
        private final Node<K,V> next;
        private volatile SpaceWrapper<V> valueWrapper;

        public Node(K key, int hash, Node<K,V> next) {
            this.key = key;
            this.hash = hash;
            this.next = next;
        }

        public Node(Node<K,V> original, Node<K,V> next) {
            this.key = original.key;
            this.hash = original.hash;
            this.next = next;
            this.valueWrapper = original.valueWrapper;
            this.TTL = original.TTL;
        }

        private long ttl() {
            return TTL;
        }

        private boolean updateTTL(long old, long ttl) {
            return Node.ttl.compareAndSet(this, old, ttl);
        }

        boolean isExpired(int now, long writeExpiry, long accessExpiry) {
            return (writeExpiry > 0 && (now - WRITE) > writeExpiry / TICK) ||
                    (accessExpiry > 0 && (now - ACCESS) > accessExpiry / TICK);
        }

        void updateElapsed(int now, boolean applyWrite, boolean applyAccess) {
            for (int t; applyWrite && (t = Node.write.get(this)) < now && ! Node.write.compareAndSet(this, t, now););
            for (int t; applyAccess && (t = Node.access.get(this)) < now && ! Node.access.compareAndSet(this, t, now););
        }

        Node<K, V> next() {
            return next;
        }

        @Override
        public K getKey() {
            return key;
        }
        @Override
        public SpaceWrapper<V> getValue() {
            return valueWrapper;
        }
        @Override
        public SpaceWrapper<V> setValue(SpaceWrapper<V> value) {
            return valueWrapper = value;
        }
        @Override
        public int hashCode() {
            return key.hashCode();
        }

    }

    static class WriteThroughEntry<K, V> implements Map.Entry<K, V> {
        final K key; // non-null
        SpaceWrapper<V> valueWrapper; // non-null
        WriteThroughEntry(K key, SpaceWrapper<V> value) {
            this.key = key;
            this.valueWrapper = value;
        }
        public K getKey() {
            return key;
        }
        public V getValue() {
            return valueWrapper.get();
        }
        public boolean equals(Object object) {
            if (object instanceof Map.Entry) {
                Map.Entry<?, ?> that = (Map.Entry<?, ?>) object;
                return key.equals(that.getKey()) && valueWrapper.equals(that.getValue());
            }
            return false;
        }
        public int hashCode() {
            return key.hashCode() ^ valueWrapper.hashCode();
        }
        public V setValue(V newValue) {
            throw new UnsupportedOperationException();
        }
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    public enum WeighStrategy {
        COUNT {
            int weightSize(Node node) {
                return 1;
            }
        }, SPACE {
            int weightSize(Node node) {
                return (int) node.valueWrapper.length();
            }
        };
        abstract int weightSize(Node node);
    }

    public enum EvictStrategy {
        FIFO {
            void updateTTL(Node node, long now, AtomicLong clock) {
                node.updateTTL(0, clock.incrementAndGet());
            }
            long weightTTL(Node node, long now) {
                return node.ttl();
            }
        }, LRU { // LRU-2
            void updateTTL(Node node, long now, AtomicLong clock) {
                long old = node.ttl();
                long tick = clock.incrementAndGet();
                long flag = (old == 0 ? 0: 1L) << 63;
                long ttl = (tick & (~0L >>> 1)) | flag;
                while ((old & (~0L >>> 1)) < tick && ! node.updateTTL(old, ttl)) {
                    old = node.ttl();
                    ttl = (tick & (~0L >>> 1)) | (1L << 63);
                }
            }
            long weightTTL(Node node, long now) {
                long ttl = node.ttl();
                long flag = ttl >>> 63;
                return flag == 0 ? 0 : ttl & (~0L >>> 1);
            }
        }, LFU { // LFU-AD
            final int TIMESTAMP_BITS = 40;
            final int FREQUENCY_BITS = 24;
            void updateTTL(Node node, long now, AtomicLong clock) {
                long old = node.ttl();
                long timestamp = old >>> FREQUENCY_BITS;
                long frequency = old & (~0L >>> TIMESTAMP_BITS);

                long elapsed = Math.min(~0L >>> FREQUENCY_BITS, now - ERA);
                while (timestamp < elapsed) {
                    double rand = ThreadLocalRandom.current().nextDouble();
                    if (1./(frequency+1) > rand) { // decay counter
                        frequency++;
                        frequency &= (~0L >>> TIMESTAMP_BITS);
                    }
                    long ttl = elapsed << FREQUENCY_BITS | frequency & (~0L >>> TIMESTAMP_BITS);
                    if (node.updateTTL(old, ttl)) {
                        break;
                    }
                    old = node.ttl();
                    timestamp = old >>> FREQUENCY_BITS;
                    frequency = old & (~0L >>> TIMESTAMP_BITS);
                }
            }
            long weightTTL(Node node, long now) {
                long ttl = node.ttl();
                long timestamp = ttl >>> FREQUENCY_BITS;
                long frequency = ttl & (~0L >>> TIMESTAMP_BITS);
                long decay = TimeUnit.MILLISECONDS.toMinutes(Math.max(now- ERA, timestamp) - timestamp);
                return frequency - decay;
            }
        };

        abstract void updateTTL(Node node, long now, AtomicLong clock);
        abstract long weightTTL(Node node, long now);
    }

    public enum RemovalCause {
        EXPLICIT {
            boolean wasEvicted() {
                return false;
            }
        },
        REPLACED {
            boolean wasEvicted() {
                return false;
            }
        },
        COLLECTED {
            boolean wasEvicted() {
                return true;
            }
        },
        EXPIRED {
            boolean wasEvicted() {
                return true;
            }
        },
        SIZE {
            boolean wasEvicted() {
                return true;
            }
        };
        abstract boolean wasEvicted();
    }


}
