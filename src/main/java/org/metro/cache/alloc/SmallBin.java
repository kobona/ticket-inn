package org.metro.cache.alloc;

import org.metro.cache.alloc.util.Stack;

import java.util.List;

public class SmallBin extends Allocator.Bin {

    public final static int BIN_DIFF = 8;
    public final static int BIN_SIZE = 32; // 32 OR 64
    public final static int MAX_SIZE = BIN_DIFF * (BIN_SIZE-1);

    /*
     * index   0    1   ...    30    31
     *  size | 8 | 16 | ... | 240 | 248 |
     **/
    private final Stack[] bin = new Stack[BIN_SIZE];

    SmallBin() {
        for (int i=0; i<BIN_SIZE; i++) {
            bin[i] = new Stack(16, null, (i+1) << 3);
        }
    }

    public void updateMap() {
        for (int i=0; i<BIN_SIZE; i++) {
            synchronized (bin[i]) {
                if (bin[i].size() > 0) {
                    set(i);
                } else {
                    clear(i);
                }
            }
        }
    }

    public void listStack(List<Stack> stacks) {
        for (Stack s: bin) {
            stacks.add(s);
        }
    }

    private long popStack(int index) {
        if (((map >> index) & 1) != 0) {
            synchronized (bin[index]) {
                Stack s = bin[index];
                int size = s.size();
                if (size > 0) {
                    if (size == 1) {
                        clear(index);
                    }
                    return s.pop();
                }
            }
        }
        return Stack.NULL;
    }

    public Allocator.Chunk tryMalloc(int size, boolean accurate) {

        long ptr = Stack.NULL;
        int index = (size >> 3) - 1;

        if (map >>> index == 0) {
            return null;
        }

        if (((map >>> index) & 0x3) != 0) {
            ptr = popStack(index);
            if (ptr == Stack.NULL) {
                ptr = popStack(++index);
            }
        }

        if (ptr != Stack.NULL) {
            return new Allocator.Chunk(ptr, (index+1)<<3);
        } else if (accurate) {
            return null;
        }

        while (ptr == Stack.NULL && ++index < BIN_SIZE && (map >>> index) != 0) {
            ptr = popStack(index);
        }

        if (ptr != Stack.NULL) {
            return new Allocator.Chunk(ptr, (index+1)<<3);
        }

        return null;
    }

    public void free(long address, long size) {
        int index = (int) ((size >> 3) - 1);
        synchronized (bin[index]) {
            Stack s = bin[index];
            s.push(address);
            if (s.size() == 1) {
                set(index);
            }
        }
    }

    public int[] stats() {
        // pair: (size, num)

        int length = MAX_SIZE / BIN_DIFF;

        int[] stats = new int[BIN_SIZE*2];
        for (int i=0; i<BIN_SIZE; i++) {
            stats[(i<<1)] = (i+1) << 3;
            stats[(i<<1)+1] = bin[i].size();
        }
        return stats;
    }
}
