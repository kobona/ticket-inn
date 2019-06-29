package org.metro.cache.serial;

import io.protostuff.CodedInput;
import io.protostuff.Input;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SpaceInput extends InputStream {

    private final BufferedInputStream buffer;
    private final CodedInput input;
    private SpaceWrapper space;
    private int position;

    public SpaceInput() {
        buffer = new BufferedInputStream(this, 2048) {
            public void reset() {
                markpos = -1;
                pos = count = 0;
            }
            public void close() {
                throw new UnsupportedOperationException();
            }
        };
        input = new CodedInput(this, false);
    }

    public SpaceInput wrap(SpaceWrapper space) {
        this.space = space;
        this.position = 0;
        return this;
    }

    public Input input() {
        input.resetSizeCounter();
        return input;
    }

    public BufferedInputStream buffer() {
        try { buffer.reset(); }
        catch (IOException e) { throw new RuntimeException(e); }
        return buffer;
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > buf.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int pos = position;
        int end = (int) Math.min(pos + len, space.size());
        if (end == pos) {
            return -1;
        }

        space.getBytes(pos, buf, off, end - pos);
        return (position = end) - pos;
    }
}
