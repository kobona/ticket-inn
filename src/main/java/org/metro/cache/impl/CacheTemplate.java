package org.metro.cache.impl;


import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Triple;
import org.metro.cache.alloc.Memory;
import org.metro.cache.serial.Serialization;
import org.metro.cache.serial.SpaceWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


/**
 * <p> Created by pengshuolin on 2019/6/12
 */
public class CacheTemplate<K,V> extends CacheStruct {

    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final int MAX_SEGMENTS = 1 << 16; // slightly conservative
    private static final int DRAIN_THRESHOLD = 0x3F;

    private final Memory memory;

    private final EvictStrategy evicting;
    private final WeighStrategy weighing;
    private final long maximumSize;
    private final long expiryAfterAccess;
    private final long expiryAfterWrite;

    private final int segmentMask;
    private final int segmentShift;
    private final Segment[] segments;

    CacheTemplate(CacheBuilder builder, Memory memory) {

        this.memory = memory;
        this.evicting = builder.evicting;
        this.weighing = builder.weighing;

        expiryAfterAccess = builder.expiryAfterAccess;
        expiryAfterWrite = builder.expiryAfterWrite;
        maximumSize = builder.maximumSize;

        int concurrencyLevel = Math.min(builder.concurrencyLevel, MAX_SEGMENTS);
        int initialCapacity = Math.min(builder.initialCapacity, MAXIMUM_CAPACITY);
        if (evictsBySize()) {
            initialCapacity = Math.min(initialCapacity, (int) maximumSize);
        }

        int segmentShift = 0;
        int segmentCount = 1;
        while (segmentCount < concurrencyLevel
                && (!evictsBySize() || segmentCount * 20 <= maximumSize)) {
            ++segmentShift;
            segmentCount <<= 1;
        }
        this.segmentShift = 32 - segmentShift;
        this.segmentMask = segmentCount - 1;
        this.segments = (Segment[]) new Object[segmentCount];

        int segmentCapacity = initialCapacity / segmentCount;
        if (segmentCapacity * segmentCount < initialCapacity) {
            ++segmentCapacity;
        }

        int segmentSize = 1;
        while (segmentSize < segmentCapacity) {
            segmentSize <<= 1;
        }

        if (evictsBySize()) {
            // Ensure sum of segment max weights = overall max weights
            long maxSegmentWeight = maximumSize / segmentCount + 1;
            long remainder = maximumSize % segmentCount;
            for (int i = 0; i < this.segments.length; ++i) {
                if (i == remainder) {
                    maxSegmentWeight--;
                }
                this.segments[i] = new Segment(initialCapacity, maxSegmentWeight);
            }
        } else {
            for (int i = 0; i < this.segments.length; ++i) {
                this.segments[i] = new Segment(initialCapacity, 0L);
            }
        }
    }

    public Memory mem() {
        return memory;
    }

    private Segment segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }

    private static int hash(Object obj) {
        int h = obj.hashCode();
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    public int size() {
        Segment[] segments = this.segments;
        long size = 0;
        for (int i = 0; i < segments.length; ++i) {
            size += segments[i].count;
        }
        if (size > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (size < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) size;
    }

    public boolean containsKey(Object key) {
        int hash = hash(Objects.requireNonNull(key));
        return segmentFor(hash).containsKey(key, hash);
    }

    public SpaceWrapper<V> loadIfAbsent(Object key, Function<K,V> loader) {
        int hash = hash(Objects.requireNonNull(key));
        return segmentFor(hash).get(key, hash, loader);
    }

    public SpaceWrapper<V> getIfPresent(Object key) {
        int hash = hash(Objects.requireNonNull(key));
        return segmentFor(hash).get(key, hash);
    }

    public Map<K, SpaceWrapper<V>> getAllPresent(Iterable<?> keys) {
        Map<K, SpaceWrapper<V>> result = new LinkedHashMap<>();
        for (Object key : keys) {
            int hash = hash(Objects.requireNonNull(key));
            SpaceWrapper<V> wrapper = segmentFor(hash).get(key, hash);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                K castKey = (K) key;
                result.put(castKey, wrapper);
            }
        }
        return result;
    }

    public V put(K key, V value, boolean retValue) {
        int hash = hash(Objects.requireNonNull(key));
        return segmentFor(hash).put(key, hash, Objects.requireNonNull(value), false, retValue);
    }

    public V putIfAbsent(K key, V value, boolean retValue) {
        int hash = hash(Objects.requireNonNull(key));
        return segmentFor(hash).put(key, hash, Objects.requireNonNull(value), true, retValue);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue(), false);
        }
    }

    public V remove(Object key, boolean retValue) {
        int hash = hash(Objects.requireNonNull(key));
        return segmentFor(hash).remove(key, hash, retValue);
    }

    public void invalidateAll(Iterable<?> keys) {
        for (Object key : keys) {
            remove(key, false);
        }
    }

    public void clear() {
        for (Segment segment : segments) {
            segment.clear();
        }
    }

    private boolean isExpired(Node<K,V> node, int now) {
        return node.isExpired(now, expiryAfterWrite, expiryAfterAccess);
    }

    private boolean recordsWrite() {
        return expiryAfterWrite > 0;
    }

    private boolean recordsAccess() {
        return expiryAfterAccess > 0;
    }

    private boolean evictsBySize() {
        return maximumSize > 0;
    }

    @SuppressWarnings("unchecked")
    private SpaceWrapper<V> wrapValue(V value) {
        return Serialization.write(value, memory);
    }

    private final Queue<Triple<K,SpaceWrapper<V>, RemovalCause>>
            removalNotificationQueue = new ConcurrentLinkedQueue<>();

    class Segment extends ReentrantLock {

        volatile int count;
        long totalWeight;
        int modCount;
        int threshold;
        volatile AtomicReferenceArray<Node<K, V>> table;
        final long maxSegmentWeight;
        final AtomicInteger readCount = new AtomicInteger();


        Segment(int initialCapacity, long maxSegmentWeight) {
            this.maxSegmentWeight = maxSegmentWeight;
            this.table = new AtomicReferenceArray<>(initialCapacity);
            this.threshold = table.length() * 3 / 4; // 0.75
            if (this.threshold == maxSegmentWeight) {
                // prevent spurious expansion before eviction
                this.threshold++;
            }
        }

        Node<K, V> copyEntry(Node<K, V> original, Node<K, V> newNext) {
            return original.getValue().free() ? null: new Node<>(original, newNext);
        }

        Node<K, V> newEntry(K key, int hash, Node<K, V> next) {
            return new Node<>(key, hash, next);
        }

        boolean containsKey(Object key, int hash) {
            try {
                if (count != 0) { // read-volatile
                    return getLiveEntry(key, hash,  elapsed()) != null;
                }
                return false;
            } finally {
                postReadCleanup();
            }
        }

        void expand() {
            AtomicReferenceArray<Node<K, V>> oldTable = table;
            int oldCapacity = oldTable.length();
            if (oldCapacity >= MAXIMUM_CAPACITY) {
                return;
            }

            int newCount = count;
            AtomicReferenceArray<Node<K, V>> newTable = new AtomicReferenceArray<>(oldCapacity << 1);
            threshold = newTable.length() * 3 / 4;
            int newMask = newTable.length() - 1;
            for (int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
                // We need to guarantee that any existing reads of old Map can
                // proceed. So we cannot yet null out each bin.
                Node<K, V> head = oldTable.get(oldIndex);

                if (head != null) {
                    Node<K, V> next = head.next();
                    int headIndex = head.hashCode() & newMask;

                    // Single node on list
                    if (next == null) {
                        newTable.set(headIndex, head);
                    } else {
                        // Reuse the consecutive sequence of nodes with the same target
                        // index from the end of the list. tail points to the first
                        // entry in the reusable list.
                        Node<K, V> tail = head;
                        int tailIndex = headIndex;
                        for (Node<K, V> e = next; e != null; e = e.next()) {
                            int newIndex = e.hashCode() & newMask;
                            if (newIndex != tailIndex) {
                                // The index changed. We'll need to copy the previous entry.
                                tailIndex = newIndex;
                                tail = e;
                            }
                        }
                        newTable.set(tailIndex, tail);

                        // Clone nodes leading up to the tail.
                        for (Node<K, V> e = head; e != tail; e = e.next()) {
                            int newIndex = e.hashCode() & newMask;
                            Node<K, V> newNext = newTable.get(newIndex);
                            Node<K, V> newFirst = copyEntry(e, newNext);
                            if (newFirst != null) {
                                newTable.set(newIndex, newFirst);
                            } else {
                                enqueueNotification(e, RemovalCause.COLLECTED);
                                newCount--;
                            }
                        }
                    }
                }
            }
            table = newTable;
            this.count = newCount;
        }

        V remove(Object key, int hash, boolean retValue) {
            lock();
            try {
                int now = elapsed();
                runLockedCleanup(now);

                AtomicReferenceArray<Node<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                Node<K, V> first = table.get(index);

                int newCount;
                for (Node<K, V> e = first; e != null; e = e.next()) {
                    K entryKey = e.getKey();
                    if (e.hashCode() == hash && Objects.equals(entryKey, key)) {
                        ++modCount;
                        Node<K, V> newFirst = removeEntryFromChain(
                                first, e, RemovalCause.EXPLICIT);
                        newCount = this.count - 1;
                        table.set(index, newFirst);
                        this.count = newCount; // write-volatile
                        return retValue ? e.getValue().get(): null;
                    }
                }

                return null;
            } finally {
                unlock();
                runUnlockedCleanup();
            }
        }

        V put(K key, int hash, V value, boolean onlyIfAbsent, boolean retValue) {

            lock();
            try {
                int now = elapsed(); // map.ticker.read();
                runLockedCleanup(now);

                int newCount = this.count + 1;
                if (newCount > this.threshold) { // ensure capacity
                    expand();
                }

                AtomicReferenceArray<Node<K, V>> table = this.table;
                int index = hash & (table.length() - 1);
                Node<K, V> first = table.get(index);

                // Look for an existing entry.
                for (Node<K, V> e = first; e != null; e = e.next()) {
                    if (e.hashCode() == hash && Objects.equals(e.getKey(), key)) {
                        // We found an existing entry.
                        if (onlyIfAbsent) {
                            recordRead(e, now);
                            return retValue ? e.getValue().get(): null;
                        } else {
                            // clobber existing entry, count remains unchanged
                            ++modCount;
                            enqueueNotification(e, RemovalCause.REPLACED);
                            setValue(e, value, now);
                            evictEntries(e);
                            return retValue ? e.getValue().get(): null;
                        }
                    }
                }

                // Create a new entry.
                ++modCount;
                Node<K, V> newEntry = newEntry(key, hash, first);
                setValue(newEntry, value, now);
                table.set(index, newEntry);
                newCount = this.count + 1;
                this.count = newCount; // write-volatile
                evictEntries(newEntry);
                return null;
            } finally {
                unlock();
                runUnlockedCleanup();
            }
        }

        void setValue(Node<K, V> entry, SpaceWrapper<V> value, int now) {
            entry.setValue(value);
            int weight = weighing.weightSize(entry);
            Validate.validState(weight >= 0, "Weights must be non-negative");
            recordWrite(entry, weight, now);
        }

        SpaceWrapper<V> get(Object key, int hash) {
            try {
                if (count != 0) { // read-volatile
                    int now = elapsed();
                    Node<K, V> e = getLiveEntry(key, hash, now);
                    if (e == null) {
                        return null;
                    }
                    SpaceWrapper<V> value = e.getValue();
                    if (value != null) {
                        recordRead(e, now);
                        hits.increment();
                        return value;
                    }
                    misses.increment();
                }
                return null;
            } finally {
                postReadCleanup();
            }
        }

        SpaceWrapper<V> get(Object key, int hash, Function<? super K, V> loader) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(loader);
            try {
                if (count != 0) { // read-volatile
                    // don't call getLiveEntry, which would ignore loading values
                    Node<K, V> e = getEntry(key, hash);
                    if (e != null) {
                        int now = elapsed();
                        if (! isExpired(e, now)) {
                            recordRead(e, now);
                            hits.increment();
                            return e.getValue();
                        }
                    }
                }
                // at this point e is either null or expired;
//                put(key, hash, false, loader.apply(key))
                return load((K) key, hash, loader);
            } finally {
                postReadCleanup();
            }
        }

        SpaceWrapper<V> load(K key, int hash, Function<? super K, V> loader) {
            Node<K, V> e;
            boolean createNewEntry = true;
            lock();
            try {
                // re-read ticker once inside the lock
                int now = elapsed();
                runLockedCleanup(now);

                int newCount = this.count - 1;
                AtomicReferenceArray<Node<K, V>> table = this.table;
                int index = hash & (table.length() - 1);

                Node<K, V> first = table.get(index);
                for (e = first; e != null; e = e.next()) {
                    K entryKey = e.getKey();
                    if (e.hashCode() == hash && Objects.equals(entryKey, key)) {
                        if (isExpired(e, now)) {
                            enqueueNotification(e, RemovalCause.EXPIRED);
                            this.count = newCount;
                        } else {
                            hits.increment();
                            return e.getValue();
                        }
                        break;
                    }
                }

                V value = loader.apply(key);
                if (e == null) {
                    e = newEntry(key, hash, first);
                    table.set(index, e);
                    misses.increment();
                }
                setValue(e, value, now);
                return e.getValue();
            } finally {
                unlock();
                runUnlockedCleanup();
            }
        }


        void clear() {
            if (count != 0) { // read-volatile
                lock();
                try {
                    AtomicReferenceArray<Node<K, V>> table = this.table;
                    for (int i = 0; i < table.length(); ++i) {
                        for (Node<K, V> e = table.get(i); e != null; e = e.next()) {
                            enqueueNotification(e, RemovalCause.EXPLICIT);
                        }
                    }
                    for (int i = 0; i < table.length(); ++i) {
                        table.set(i, null);
                    }
                    readCount.set(0);
                    ++modCount;
                    count = 0; // write-volatile
                } finally {
                    unlock();
                    runUnlockedCleanup();
                }
            }
        }


        void recordRead(Node<K, V> entry, int now) {
            if (recordsAccess()) {
                entry.updateElapsed(now, false, recordsAccess());
            }
            if (evictsBySize()) {
                evicting.updateTTL(entry, now, CLOCK);
            }
        }

        void recordWrite(Node<K, V> entry, int weight, int now) {
            totalWeight += weight;
            if (recordsAccess() || recordsWrite()) {
                entry.updateElapsed(now, recordsWrite(), recordsAccess());
            }
            if (evictsBySize()) {
                evicting.updateTTL(entry, now, CLOCK);
            }
        }

        void postReadCleanup() {
            if ((readCount.incrementAndGet() & DRAIN_THRESHOLD) == 0) {
                runLockedCleanup(elapsed());
                runUnlockedCleanup();
            }
        }

        void runLockedCleanup(int now) {
            if (tryLock()) {
                try {
                    expireEntries(now);
                    readCount.set(0);
                } finally {
                    unlock();
                }
            }
        }

        void runUnlockedCleanup() {
            // locked cleanup may generate notifications we can send unlocked
            if (!isHeldByCurrentThread()) {
                processPendingNotifications();
            }
        }

        void evictEntries(Node<K, V> newest) {
            if (! evictsBySize()) { // need evict by size
                return;
            }

            if (newest != null && weighing.weightSize(newest) > maxSegmentWeight) {
                if (!removeEntry(newest, newest.hashCode(), RemovalCause.SIZE)) {
                    throw new AssertionError();
                }
            }

            int now = elapsed();
            Comparator<Node> comparator = new Comparator<Node>() {
                public int compare(Node o1, Node o2) {
                    long w1 = evicting.weightTTL(o1, now);
                    long w2 = evicting.weightTTL(o2, now);
                    return -Long.compare(w1, w2);
                }
            };

            PriorityQueue<Node<K,V>> queue = new PriorityQueue<>(comparator);

            int visited = -1;
            AtomicReferenceArray<Node<K, V>> table = this.table;
            while (totalWeight > maxSegmentWeight) {
                long overweight = totalWeight - maxSegmentWeight;

                int index = ThreadLocalRandom.current().nextInt(table.length());
                while (index == visited && table.length() > 1) {
                    index = ThreadLocalRandom.current().nextInt(table.length());
                }

                Node<K, V> first = table.get(index);
                for (Node<K, V> e = first; e != null; e = e.next()) {
                    int w = weighing.weightSize(e);
                    if (overweight > 0) {
                        queue.add(e);
                        overweight -= w;
                    } else {
                        Node top = queue.peek();
                        if (comparator.compare(e, top) < 1) {
                            int topW = weighing.weightSize(top);
                            if (overweight - topW + w < 0) {
                               queue.poll();
                            }
                           queue.add(e);
                        }
                    }
                }

                while (totalWeight > maxSegmentWeight && ! queue.isEmpty()) {
                    Node<K,V> node = Objects.requireNonNull(queue.poll());
                    removeEntry(node, node.hashCode(), RemovalCause.SIZE);
                }

                queue.clear();
                visited = index;
            }
        }

        boolean removeEntry(Node<K, V> entry, int hash, RemovalCause cause) {
            AtomicReferenceArray<Node<K, V>> table = this.table;
            int index = hash & (table.length() - 1);
            Node<K, V> first = table.get(index);

            int newCount;
            for (Node<K, V> e = first; e != null; e = e.next()) {
                if (e == entry) {
                    ++modCount;
                    Node<K, V> newFirst = removeEntryFromChain(first, e, cause);
                    newCount = this.count - 1;
                    table.set(index, newFirst);
                    this.count = newCount; // write-volatile
                    return true;
                }
            }

            return false;
        }

        Node<K, V> removeEntryFromChain(Node<K, V> first,
                                        Node<K, V> entry,
                                        RemovalCause cause) {

            enqueueNotification(entry, cause);

            int newCount = count;
            Node<K, V> newFirst = entry.next();
            for (Node<K, V> e = first; e != entry; e = e.next()) {
                Node<K, V> next = copyEntry(e, newFirst);
                if (next != null) {
                    newFirst = next;
                } else {
                    enqueueNotification(entry, RemovalCause.COLLECTED);
                    newCount--;
                }
            }
            this.count = newCount;
            return newFirst;
        }


        Node<K, V> getLiveEntry(Object key, int hash, int now) {
            Node<K, V> e = getEntry(key, hash);
            if (e == null) {
                return null;
            } else if (isExpired(e, now)) {
                tryExpireEntries(now);
                return null;
            }
            return e;
        }

        Node<K, V> getEntry(Object key, int hash) {
            for (Node<K, V> e = getFirst(hash); e != null; e = e.next()) {
                if (e.hashCode() != hash) {
                    continue;
                }
                if (Objects.equals(key, e.getKey())) {
                    return e;
                }
            }
            return null;
        }

        Node<K, V> getFirst(int hash) {
            // read this volatile field only once
            AtomicReferenceArray<Node<K, V>> table = this.table;
            return table.get(hash & (table.length() - 1));
        }

        void tryExpireEntries(int now) {
            if (tryLock()) {
                try {
                    expireEntries(now);
                } finally {
                    unlock();
                    // don't call postWriteCleanup as we're in a read
                }
            }
        }

        void expireEntries(int now) {
            AtomicReferenceArray<Node<K, V>> table = this.table;
            for (int i=0; i<table.length(); i++) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    expireTable(table, i, now);
                }
            }
        }

        void expireTable(AtomicReferenceArray<Node<K, V>> table, int index, int now) {
            Node<K, V> first = table.get(index);
            int newCount;
            for (Node<K, V> e = first; e != null; e = e.next()) {
                if (isExpired(e, now)) {
                    ++modCount;
                    Node<K, V> newFirst = removeEntryFromChain(first, e, RemovalCause.EXPIRED);
                    newCount = this.count - 1;
                    table.set(index, newFirst);
                    this.count = newCount; // write-volatile
                }
            }
        }

        void enqueueNotification(Node<K, V> entry, RemovalCause cause) {
            totalWeight -= weighing.weightSize(entry);
            if (cause.wasEvicted()) {
                removalNotificationQueue.offer(Triple.of(entry.getKey(), entry.getValue(), cause));
                eviction.increment();
            }
        }

    }

    void processPendingNotifications() {
        Triple<K,SpaceWrapper<V>, RemovalCause> notification;
        while ((notification = removalNotificationQueue.poll()) != null) {
            memory.release(notification.getMiddle());
        }
    }

    private Set<K> keySet;

    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet());
    }

    public Iterator<K> keys() {
        return new KeyIterator();
    }

    public Iterator<V> values() {
        return new ValueIterator();
    }

    public Iterator<Map.Entry<K, V>> entries() {
        return new EntryIterator();
    }

    final class KeyIterator extends HashIterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    final class ValueIterator extends HashIterator<V> {
        public V next() {
            return nextEntry().getValue();
        }
    }
    final class EntryIterator extends HashIterator<Map.Entry<K, V>> {
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    final class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }
        public int size() {
            return CacheTemplate.this.size();
        }
        public void clear() {
            throw new UnsupportedOperationException();
        }
        public boolean contains(Object o) {
            return CacheTemplate.this.containsKey(o);
        }
        public boolean remove(Object o) {
            return CacheTemplate.this.remove(o, true) != null;
        }
        public Object[] toArray() {
            return toArrayList().toArray();
        }
        public <E> E[] toArray(E[] a) {
            return toArrayList().toArray(a);
        }
        private ArrayList<K> toArrayList() {
            ArrayList<K> result = new ArrayList<>(size());
            iterator().forEachRemaining(result::add);
            return result;
        }
    }

    abstract class HashIterator<T> implements Iterator<T> {

        int nextSegmentIndex;
        int nextTableIndex;
        Segment currentSegment;
        AtomicReferenceArray<Node<K,V>> currentTable;
        Node<K,V> nextEntry;
        WriteThroughEntry<K,V> nextExternal;
        WriteThroughEntry<K,V> lastReturned;

        HashIterator() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            advance();
        }

        @Override
        public abstract T next();

        final void advance() {
            nextExternal = null;
            if (nextInChain()) {
                return;
            }
            if (nextInTable()) {
                return;
            }
            while (nextSegmentIndex >= 0) {
                currentSegment = segments[nextSegmentIndex--];
                if (currentSegment.count != 0) {
                    currentTable = currentSegment.table;
                    nextTableIndex = currentTable.length() - 1;
                    if (nextInTable()) {
                        return;
                    }
                }
            }
        }
        boolean nextInChain() {
            if (nextEntry != null) {
                for (nextEntry = nextEntry.next(); nextEntry != null; nextEntry = nextEntry.next()) {
                    if (advanceTo(nextEntry)) {
                        return true;
                    }
                }
            }
            return false;
        }
        boolean nextInTable() {
            while (nextTableIndex >= 0) {
                if ((nextEntry = currentTable.get(nextTableIndex--)) != null) {
                    if (advanceTo(nextEntry) || nextInChain()) {
                        return true;
                    }
                }
            }
            return false;
        }
        boolean advanceTo(Node<K, V> entry) {
            try {
                K key = entry.getKey();
                SpaceWrapper<V> wrapper = entry.getValue();
                if (isExpired(entry, elapsed()) && !wrapper.free()) {
                    nextExternal = new WriteThroughEntry<>(key, wrapper);
                    return true;
                } else {
                    // Skip stale entry.
                    return false;
                }
            } finally {
                currentSegment.postReadCleanup();
            }
        }

        WriteThroughEntry<K,V> nextEntry() {
            if (nextExternal == null) {
                throw new NoSuchElementException();
            }
            lastReturned = nextExternal;
            advance();
            return lastReturned;
        }

        @Override
        public boolean hasNext() {
            return nextExternal != null;
        }

        @Override
        public void remove() {
            Validate.validState(lastReturned != null);
            CacheTemplate.this.remove(lastReturned.getKey(), false);
            lastReturned = null;
        }
    }
}
