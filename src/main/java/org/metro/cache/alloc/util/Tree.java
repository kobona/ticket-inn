package org.metro.cache.alloc.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * SkipList
 * */
@SuppressWarnings("unchecked")
public class Tree {

    private static final int MAX_LV = 5;
    private static final double P = 0.25;

    private static int random() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int level = 1;
        while (random.nextDouble() < P && level < MAX_LV) level++;
        return level;
    }

    static final class Node{

        final long key;
        Object value;
        final Node[] next;

        Node(int level, long key, Object value) {
            this.key = key;
            this.value = value;
            next = new Node[level];
        }

    }

    public final class TreeIter {
        Node node;
        TreeIter(long from) { node = (size == 0)? null: predecessor(head, from, 0); }
        public long key() {
            return node.key;
        }
        public <V> V value() {
            return (V) node.value;
        }
        public boolean next() {
            return node != null && (node = node.next[0]) != null;
        }

    }

    public final static long ROOT_KEY = Long.MIN_VALUE;

    private final Node head = new Node(MAX_LV, ROOT_KEY, null);
    private int size = 0, level = 1;

    private static Node predecessor(Node x, long key, int level) {
        while (x.next[level] != null && x.next[level].key < key)
            x = x.next[level];
        return x;
    }

    private Node search(long key, Node[] prev) {
        if (key < 0)
            throw new IllegalArgumentException();

        Node x = head;
        for (int i=level-1; i>=0; i--) {

            x = predecessor(x, key, i);

            if (prev != null) prev[i] = x;

            if (x.next[i] != null && x.next[i].key == key) {

                Node n = x; x = x.next[i];
                while (--i >= 0)
                    if (prev != null)
                        prev[i] = (n = predecessor(n, key, i));

            }
        }
        return x.key == key? x: null;
    }



    private Object delete(Node x, Node[] prev) {
        for (int i=0; i<level; i++) {
            if (prev[i].next[i] == x) {
                prev[i].next[i] = x.next[i];
            }
        }
        while (level > 1 && head.next[level-1] == null) level--;
        size--;
        return x.value;
    }

    public <T> T get(long key) {
        Node n = search(key, null);
        return n == null? null: (T) n.value;
    }

    public <T> T put(long key, T value) {

        Node[] prev = new Node[MAX_LV];
        Node n = search(key, prev);

        if (n != null) {
            T t = (T) n.value;
            n.value = value;
            return t;
        }

        int lv = random();
        if (lv > level) {
            while (level < lv) prev[level++] = head;
        }

        n = new Node(lv, key, value);
        for (int i=0; i<lv; i++) {
            n.next[i] = prev[i].next[i];
            prev[i].next[i] = n;
        }

        size++;
        return null;
    }

    public <T> T remove(long key) {
        Node[] prev = new Node[MAX_LV];
        Node n = search(key, prev);
        return (n == null) ? null: (T) delete(n, prev);
    }

    public int size() {
        return size;
    }

    public TreeIter iter(long from) { return new TreeIter(from); }

    public <R> R call(Function<Tree, R> func) {
        synchronized (head) {
            return func.apply(this);
        }
    }

    public void exec(Consumer<Tree> func) {
        synchronized (head) {
            func.accept(this);
        }
    }

}
