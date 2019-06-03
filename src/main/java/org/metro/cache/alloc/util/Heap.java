package org.metro.cache.alloc.util;


/**
 * Fix-size MinHeap
 * */
public class Heap<T extends Comparable> implements Comparable<Heap<T>> {

    private int size = 0;
    private final Comparable[] tree;

    public Heap(int capacity) {
        this.tree = new Comparable[capacity];
    }

    private static int parent(int node) {
        return ((node + 1) >> 1) - 1;
    }
    private static int left(int node) {
        return (node << 1) + 1;
    }
    private static int right(int node) {
        return (node << 1) + 2;
    }

    private int compare(int a, int b) {
        return tree[a].compareTo(tree[b]);
    }
    private void swap(int a, int b) {
        Comparable t = tree[a];
        tree[a] = tree[b];
        tree[b] = t;
    }

    private void shiftUp(int end) {
        int cursor = end - 1;
        if (cursor <= 0) return;

        int parent = parent(cursor);
        while (parent >= 0) {

            if (compare(cursor, parent) >= 0)
                return;

            swap(cursor, parent);

            parent = parent(cursor = parent);
        }
    }

    private void shiftDown(int end) {
        if (end == 0) return;

        int cursor = 0;
        while (cursor < end) {

            int left = left(cursor), right = right(cursor);

            int child = -1;
            if (left < end && right < end) {
                child = (compare(left, right) < 0) ? left: right;
            } else if (left < end) {
                child = left;
            }

            if (child == -1 || compare(cursor, child) < 0)
                return;

            swap(cursor, child);

            cursor = child;
        }
    }

    public void push(T t) {
        tree[size] = t;
        shiftUp(++size);
    }

    public T pop() {

        if (size == 0)
            return null;

        T t = (T) tree[0];
        if (--size > 0) {
            tree[0] = tree[size];
            shiftDown(size);
        }
        return t;
    }

    public T top() {
        return (T) tree[0];
    }

    @Override
    public int compareTo(Heap o) {
        return top().compareTo(o.top());
    }
}