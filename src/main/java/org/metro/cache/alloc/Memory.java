package org.metro.cache.alloc;

import org.metro.cache.alloc.util.Align;
import org.metro.cache.alloc.util.Unsafe;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.UUID;

public final class Memory extends Unsafe.Accessor implements Closeable {

    static final Method map0;
    static {
        try {
            map0 = sun.nio.ch.FileChannelImpl.class.getDeclaredMethod("map0", int.class, long.class, long.class);
            map0.setAccessible(true);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private final RandomAccessFile file;
    private final long address, length;
    private final Allocator allocator;

    public Memory(long length) throws IOException {
        this(UUID.randomUUID().toString().replace("-", ""), length);
    }

    public Memory(String name, long length) throws IOException {
        this(name, length, true, null);
    }

    public Memory(String name, long length, boolean autoDel, Allocator.SpaceFactory factory) throws IOException {

        File file = new File(name);
        if (autoDel) file.deleteOnExit();

        this.file = new RandomAccessFile(file, "rw");
        this.file.setLength(this.length = Align.UP.alignPage(length));

        try {
            this.address = (long) map0.invoke(this.file.getChannel(), 1, 0L, this.length);
            this.allocator = new Allocator(name, address, this.length, factory);
        } catch (Exception e) {
            throw new Error(e);
        }

    }

    public long address() {
        return address;
    }

    public long length() {
        return length;
    }

    public void close() throws IOException {
        file.close();
    }

    public Allocator.Space acquire(int size) {
        return allocator.malloc(size);
    }

    public void release(Allocator.Space space) {
        allocator.free(space);
    }

    public Reporter reporter() {
        return allocator.report();
    }
}
