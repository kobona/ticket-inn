package org.metro.cache.alloc;


import java.util.concurrent.atomic.AtomicReference;

/**
 * Fast Memory Keeper
 * */
public class LastRemain {

    private final AtomicReference<Allocator.Chunk> holder = new AtomicReference<>();

    public long size() {
        Allocator.Chunk chunk = holder.get();
        return  (chunk == null)? 0: chunk.size;
    }

    public Allocator.Chunk hold(long address, long size) {
        if (size == 0) return null;

        Allocator.Chunk next = new Allocator.Chunk(address, size);
        while (true) {
            Allocator.Chunk prev = holder.get();
            if (prev == null || prev.size < next.size) {
                if (holder.compareAndSet(prev, next))
                    return prev;
            } else break;
        }
        return next;
    }

    public Allocator.Chunk[] split(int size, int threshold) {
        while (true) {

            Allocator.Chunk chunk = holder.get();
            if (chunk == null || chunk.size < size) {
                return null;
            }

            if (chunk.size - size <= 8) {
                if (holder.compareAndSet(chunk, null))
                    return new Allocator.Chunk[]{chunk};
                continue;
            }

            Allocator.Chunk rest = new Allocator.Chunk(chunk.address+size, chunk.size-size);
            if (rest.size > threshold) {
                if (holder.compareAndSet(chunk, rest))
                    return new Allocator.Chunk[]{new Allocator.Chunk(chunk.address, size)};
                continue;
            }

            if (holder.compareAndSet(chunk, null)) {
                return new Allocator.Chunk[]{new Allocator.Chunk(chunk.address, size), rest};
            }
        }
    }

}
