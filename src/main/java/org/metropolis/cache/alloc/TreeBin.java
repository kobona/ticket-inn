package org.metropolis.cache.alloc;

import org.metropolis.cache.alloc.Allocator.Chunk;
import org.metropolis.cache.alloc.util.Stack;
import org.metropolis.cache.alloc.util.Tree;
import org.metropolis.cache.alloc.util.Tree.TreeIter;

import java.util.List;

public class TreeBin extends Allocator.Bin {

    final static int BIN_SIZE = 32; // 64
    final static int BIN_BASE = SmallBin.BIN_DIFF * SmallBin.BIN_SIZE; // 256 OR 512
    final static int BIN_SHIFT = Integer.numberOfTrailingZeros(BIN_BASE);

    static int tableIndex(long size) {
        int log = 62 - Long.numberOfLeadingZeros(size >>> (BIN_SHIFT-1));
        long idx = (log<<1) + ((size >>> (log+BIN_SHIFT-1)) & 1);
        return (idx >= BIN_SIZE)? BIN_SIZE-1: (int)idx;
    }

    private final Tree[] bin = new Tree[BIN_SIZE];

    TreeBin() {
        for (int i=0; i<BIN_SIZE; i++) {
            bin[i] = new Tree();
        }
    }

    /** ThreadUnsafe*/
    public void updateMap() {
        for (int i=0; i<BIN_SIZE; i++) {
            if (bin[i].size() > 0) {
                set(i);
            } else {
                clear(i);
            }
        }
    }

    /** ThreadUnsafe*/
    public void listStack(List<Stack> stacks) {
        for (Tree t: bin) {
            for (TreeIter i = t.iter(Tree.ROOT_KEY); i.next(); stacks.add(i.value()));
        }
    }

    private Chunk popTree(int index, int size) {
        if (((map >> index) & 1) != 0) {
            return bin[index].call(t -> {
                TreeIter i = t.iter(size);
                while (i.next()) {
                    Stack s = i.value();
                    if (s.size() > 0) {
                        Chunk c = new Chunk(s.pop(), i.key());
                        if (s.size() == 0) {
                            t.remove(s.mark());
                            if (t.size() == 0)
                                clear(index);
                        }
                        return c;
                    }
                }
                return null;
            });
        }
        return null;
    }

    public Chunk tryMalloc(int size) {

        int index = tableIndex(size);
        if (map >>> index == 0) {
            return null;
        }

        Chunk chunk = null;
        while (chunk == null && ++index < BIN_SIZE && (map >>> index) != 0) {
            chunk = popTree(index, size);
        }
        return chunk;
    }

    public void free(long address, long size) {
        int index = tableIndex(size);
        bin[index].exec(t -> {
            Stack s = t.get(size);
            if (s == null) {
                t.put(size, s = new Stack(2, t, size));
                if (t.size() == 1)
                    set(index);
            }
            s.push(address);
        });
    }

    public long[] stats() {
        // tuple: (size, num, total)
        long[] stats = new long[BIN_SIZE<<2];
        for (int i=0; i<BIN_SIZE; i++) {
            long size = BIN_BASE << ((i - (i & 1)) >> 1);
             size |= (size >> (i & 1));

            final int base = (i << 2);
            stats[base] = size;

            bin[i].exec((t) -> {
                if ((stats[base + 1] = t.size()) == 0)
                    return;

                TreeIter ti = t.iter(Long.MIN_VALUE);
                while (ti.next()) {
                    Stack s = ti.value();
                    stats[base + 2] += s.size();
                    stats[base + 3] += s.size() * ti.key();
                }
            });

        }
        return stats;
    }

    /** The Document Of TreeBin */
    public static void main(String[] args) {

//        int[] x = {256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096};
//        for (int n: x){
//            System.out.println(Integer.toBinaryString(n) + "\t" + Integer.highestOneBit(n) );
//        }

        int[] sizes = new int[BIN_SIZE];

        StringBuilder sb = new StringBuilder();
        int base = BIN_BASE, size = BIN_SIZE;
        int KB = 1 << 10, MB = 1 << 20;
        for (int i=0; i<(size/2); i++) {
            int a = base << i;
            int b = a | (a >> 1);
            sb.append("\t" + i*2);
            sb.append("\t" + a);
            sb.append("/" +
                    ((a < KB)? a: (a < MB)? a/KB: a/MB) +
                    ((a < KB)? "": (a < MB)? "KB": "MB"));
            sb.append("\t" + b);
            sb.append("/" +
                    ((b < KB)? b: (b < MB)? b/KB: b/MB) +
                    ((b < KB)? "": (a < MB)? "KB": "MB"));
            sb.append("\n");

            sizes[i*2] = a;
            sizes[i*2+1] = b;

            a = b;
            b = base << (i + 1);
            sb.append("\t" + (i*2+1));
            sb.append("\t" + a);
            sb.append("/" +
                    ((a < KB)? a: (a < MB)? a/KB: a/MB) +
                    ((a < KB)? "": (a < MB)? "KB": "MB"));
            sb.append("\t" + b);
            sb.append("/" +
                    ((b < KB)? b: (b < MB)? b/KB: b/MB) +
                    ((b < KB)? "": (a < MB)? "KB": "MB"));
            sb.append("\n");

        }
        System.out.println(sb);

        for (int s: sizes){
            System.out.println(s + " " +  + tableIndex(s));
        }
    }


}
