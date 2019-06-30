package org.metropolis.cache.alloc.util;

import java.io.Closeable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;

/**
 * Dynamic Primitive Long Array
 * */
public class Stack {

    public final static long NULL = ~0;

    public final class StackIter implements Comparable<StackIter>, Closeable {

        final int stamp = size;
        boolean done = false;
        long value = NULL;
        int cursor = -1;

        public boolean next() {
            if (cursor + 1 < size) {
                value = array[++cursor];
                return true;
            }
            return false;
        }

        public void detach() { done = true; }

        public boolean done() { return done; }

        public long mark() { return mark; }

        public int cursor() { return cursor; }

        public Stack owner() { return Stack.this; }

        public long value() { return value; }

        public void clear() { this.value = (array[cursor] = Stack.NULL); }

        @Override
        public void close() {
            if (!this.done)
                throw new IllegalStateException("iter not done yet");
            if (this.stamp != size)
                throw new ConcurrentModificationException("stack change before iter close");
            if (this.cursor == size)
                throw new IllegalStateException("iter closed");
            evictNull(true);
            this.cursor = size;
            this.value = NULL;
        }

        @Override
        public int compareTo(StackIter o) {
            return Long.compareUnsigned(value, o.value);
        }
    }

    private final Object domain;
    private final long mark;
    private long[] array;
    private int size = 0;

    public Stack(int initCapacity, Object domain, long mark) {
        array = new long[initCapacity];
        this.domain = domain;
        this.mark = mark;
    }

    public int size() {
        return size;
    }

    public void push(long value) {
        ensureRoomFor(1);
        array[size++] = value;
    }

    public long pop() {
        long value = array[size - 1];
        --size;
        return value;
    }

    public void clear(int cursor, long expected) {
        if (array[cursor] != expected)
            throw new IllegalStateException("inconsistent state");
        array[cursor] = Stack.NULL;
    }

    public StackIter iter(boolean sort) {
        if (sort && size > 0) {
            Arrays.sort(array, 0, size);
        }
        return new StackIter();
    }

    public long mark() {
        return mark;
    }

    public Object domain() {
        return domain;
    }

    private void ensureRoomFor(int numToAdd) {
        int newCount = size + numToAdd;
        if (newCount > array.length) {
            array = Arrays.copyOf(array, growCapacity(array.length, newCount));
        } else if (newCount <= 0) {
            throw new NegativeArraySizeException(String.format("%d + %d = %d", size, numToAdd, newCount));
        }
    }

    public static int growCapacity(int oldCapacity, int minCapacity) {
        int newCapacity = oldCapacity + (oldCapacity >> 1) + 1;
        if (newCapacity < minCapacity) {
            newCapacity = Integer.highestOneBit(minCapacity - 1) << 1;
        }
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE; // guaranteed to be >= newCapacity
        }
        return newCapacity;
    }

    private void evictNull(boolean trim) {
        int i = -1;
        while (++i < size && array[i] != NULL);

        int j = i;
        while (i < size) {
            if (array[i] != NULL)
                array[j++] = array[i];
            i++;
        }

        if (trim && j < (size >> 1) && size>64) {
            int k = Math.max(4, growCapacity(j, j));
            if (k < size) {
                long[] copy = new long[k];
                System.arraycopy(array, 0, copy, 0, j);
                array = copy;
            }
        }

        size = j;
    }

    public String toString() {
        if (size < 1) return "[]";
        StringBuilder b = new StringBuilder().append('[');
        for (int i = 0; ; i++) {
            if (i == size)
                return b.append(']').toString();
            b.append(", ");
        }

    }

}
