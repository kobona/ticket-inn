package org.metro.cache.alloc;

import org.metro.cache.alloc.Detector.*;
import org.meteorite.cache.alloc.util.*;
import org.metro.cache.alloc.util.*;
import org.metro.cache.alloc.util.Stack.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Allocator with dlmalloc algorithm
 * */
public class Allocator {

    /**
     * Bin with BitMap
     * */
    static abstract class Bin {
        volatile int map;
        private final static Unsafe.BitsHolder U = new Unsafe.BitsHolder(Bin.class, "map");
        void set(int index) {  while (!U.cas1(this, map, index)); }
        void clear(int index) { while (!U.cas0(this, map, index)); }
        int bits() { return U.bits(); }
    }

    /**
     * Immutable pair of (address, size)
     * */
    static final class Chunk {
        final long address, size;
        Chunk(long address, long size) {
            this.address = address;
            this.size = size;
        }
    }

    /**
     * Resource Exposure
     * */
    public final class Space extends Unsafe.Accessor {
        private final Ref ref; // todo: there is a cycle reference
        private Space(long address, int size, Detector detector) {
            this.ref = detector.attach(this, address, size);
        }
        public final long length() {
            return ref.size;
        }
        protected final long address() {
            return ref.address;
        }
        public final Allocator allocator() { return Allocator.this; }
    }

    private final long address, size;
    private final Trap trap = new Trap(this::collectFragment, 100);
    private final Detector detector;
    private final Reporter reporter;

    final LastRemain lastRemain = new LastRemain();
    final SmallBin smallBin = new SmallBin();
    final TreeBin treeBin = new TreeBin();

    public Allocator(String name, long address, long size) {
        if (address < 0 || size <= 0) {
            throw new IllegalArgumentException("address < 0 || size <= 0");
        }
        lastRemain.hold(this.address = address, this.size = size);
        reporter = new Reporter(name, this);
        detector = new Detector(this);
    }

    int active() { return trap.active(); }

    int blocked() { return trap.blocked(); }

    public long size() { return size; }

    public Reporter report() {
        return reporter;
    }

    public Space malloc(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("size <= 0");
        if (size >= this.size)
            throw new IllegalArgumentException("size >= allocator.size");

        Space space = allocate(size);
        if (space == null && trap.trigger()) {
             space = allocate(size);
        }

        reporter.alloc(
                space != null? space.ref.size: size, space != null);

        return space;
    }

    private Space allocate(int size) {
        trap.enter();
        Chunk chunk = search(size);
        trap.quit();

        return (chunk == null)? null:
                new Space(chunk.address, (int)chunk.size, detector);
    }

    public void free(Space space) {
        if (space == null)
            throw new NullPointerException("space");
        if (space.allocator() != this)
            throw new IllegalArgumentException("space.allocator != this");
        if (space.ref.free())
            throw new IllegalStateException("space is free");
        free(space.ref);
        reporter.free(space.ref.size);
    }

    void free(Ref ref) {
        if (ref.free()) return;
        free(ref.address, ref.size);
        ref.address = Stack.NULL;
    }

    private void free(long address, long size) {
        trap.enter();
        recycle(address, size);
        trap.quit();
    }

    private void recycle(Chunk rest) {
        if (rest == null) return;
        recycle(rest.address, rest.size);
    }

    private void recycle(long address, long size) {
        if (size <= SmallBin.MAX_SIZE) {
            smallBin.free(address, size);
        } else {
            treeBin.free(address, size);
        }
    }

    private Chunk search(int size) {
        if (size < 0)
            throw new IllegalArgumentException("size < 0");
        if (size > (1<<30))
            throw new IllegalArgumentException("size > 1GB");

        Chunk chunk, need, rest;

        size = Align.UP.align8(size);
        if (size <= SmallBin.MAX_SIZE) {
            // 1. try to allocate from small bin (using the closet two bin)
            need = smallBin.tryMalloc(size, true);
            if (need != null)
                return need;

            // 2. try to allocate from last remainder
            if (lastRemain.size() > size) {
                Chunk[] chunks = lastRemain.split(size, size);
                if (chunks != null) {
                    need = chunks[0];
                    rest = (chunks.length > 1)? chunks[1]: null;
                    recycle(rest);
                    return need;
                }
            }

            // 3. try to allocate from small bin (using the closet bin)
            chunk = smallBin.tryMalloc(size, false);
            if (chunk != null) {
                need = new Chunk(chunk.address, size);
                rest = lastRemain.hold(chunk.address+size, chunk.size-size);
                recycle(rest);
                return need;
            }

            // 4. try to allocate from tree bin
            chunk = treeBin.tryMalloc(size);
            if (chunk != null) {
                need = new Chunk(chunk.address, size);
                rest = lastRemain.hold(chunk.address+size, chunk.size-size);
                recycle(rest);
                return need;
            }


        } else {
            // 1. try to allocate from tree bin
            chunk = treeBin.tryMalloc(size);
            if (chunk != null) {
                need = new Chunk(chunk.address, size);
                rest = lastRemain.hold(chunk.address+size, chunk.size-size);
                recycle(rest);
                return need;
            }

            // 2. try to allocate from last remainder
            if (lastRemain.size() > size) {
                Chunk[] chunks = lastRemain.split(size, size);
                if (chunks != null) {
                    need = chunks[0];
                    rest = (chunks.length > 1)? chunks[1]: null;
                    recycle(rest);
                    return need;
                }
            }
        }

        // fail
        return null;
    }

    void collectFragment() {

        reporter.collFrag();

        List<Stack> stacks = new ArrayList<>(SmallBin.BIN_SIZE);
        smallBin.listStack(stacks);
        treeBin.listStack(stacks);

        // iterate all stack in order of address
        Heap<StackIter> heap = new Heap<>(stacks.size());
        for (Stack stack: stacks) {
            if (stack.size() > 0) {
                StackIter iter = stack.iter(true);
                if (iter.next()) heap.push(iter);
            }
        }

        // use stack to preserve (address, size) pair
        Stack collector = new Stack(256, null, Stack.NULL);
        boolean connected = false;

        StackIter topIter = null;
        int maxCursor = -1, topCursor = -1;
        long maxSize = lastRemain.size(), topSize = 0;
        long topPtr = Stack.NULL, nextPtr = Stack.NULL;
        while (true) {
            StackIter iter = heap.pop();
            if (iter == null) break;

            // merge adjacent fragments
            if (nextPtr == iter.value()) {
                if (!connected) {
                    connected = true;
                    topIter.owner().clear(topCursor, topPtr);
                    if (topIter.done())
                        closeIter(topIter);
                    topIter = null;
                }
                topSize += iter.mark();
                nextPtr = iter.value() + iter.mark();
                iter.clear(); // remove the address from stack
            } else {
                // collect fragment merged fragment exist
                if (connected) {
                    collector.push(topSize);
                    collector.push(topPtr);
                    if (topSize > maxSize) {
                        maxSize = topSize;
                        maxCursor = collector.size() - 2;
                    }
                }
                if (topIter != null && topIter.done()) {
                    closeIter(topIter);
                }
                connected = false;
                topCursor = (topIter = iter).cursor();
                nextPtr = (topPtr = iter.value()) + (topSize = iter.mark());
            }

            // skip to next position
            if (iter.next()) {
                heap.push(iter);
            } else {
                iter.detach();
                // once topIter is closed, the topCursor will be invalid
                // because the elem will be moved while closing
                if (iter != topIter)
                    closeIter(iter);
            }
        }

        if (connected) {
            collector.push(topSize);
            collector.push(topPtr);
            if (topSize > maxSize) {
                maxCursor = collector.size() - 2;
            }
        }
        if (topIter != null && topIter.done()) {
            closeIter(topIter);
        }

        // once all iterations were done, it will be safe to recycle the fragment
        while (collector.size() > 0) {
            long ptr = collector.pop(), size = collector.pop();
            if (collector.size() == maxCursor) {
                Chunk hold = lastRemain.hold(ptr, size);
                if (hold != null && hold.size > 0)
                   recycle(hold.address, hold.size);
            } else {
                recycle(ptr, size);
            }
        }

        smallBin.updateMap();
        treeBin.updateMap();
    }

    private void closeIter(Stack.StackIter iter) {
        // once iterator of stack is closed, the null value will be evicted
        iter.close();
        Stack s = iter.owner();
        Object o = s.domain();
        // if the stack of tree is empty, remove it from tree to reduce search cost
        if (s.size() == 0 && o instanceof Tree) {
            ((Tree) o).exec(t->t.remove(s.mark()));
        }
    }

}
