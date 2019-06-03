package org.metro.cache.serial;

import org.metro.cache.alloc.Allocator;
import io.protostuff.LinkedBuffer;

import java.io.IOException;
import java.io.OutputStream;

public class SpaceOutput extends OutputStream {

    private final LinkedBuffer buffer = LinkedBuffer.allocate(4096);
    private Allocator.Space space;
    private int position;

    public SpaceOutput wrap(Allocator.Space space) {
        this.space = space;
        this.position = 0;
        return this;
    }

    public LinkedBuffer buffer() {
        return buffer;
    }

    @Override
    public void write(int b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > buf.length) || (len < 0) ||
                ((off + len) > buf.length) || ((off + len) < 0) ||
                position + len > space.length()) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        space.putBytes(position, buf, off, len);
        position += len;
    }
}
