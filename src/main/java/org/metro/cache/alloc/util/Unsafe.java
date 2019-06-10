package org.metro.cache.alloc.util;

import java.lang.reflect.Field;

/**
 * Utils with Unsafe
 */
public class Unsafe {

    @SuppressWarnings("all")
    private static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public enum Align {

        UP {
            int align(int v, int base) { return (v+base-1) & ~(base-1); }
            long align(long v, long base) { return (v+base-1) & ~(base-1); }
            public int align4(int v) { return align(v, 4); }
            public int align8(int v) { return align(v, 8); }
            public int alignPage(int v) { return align(v, PAGE_SIZE); }
            public long alignPage(long v) { return align(v, PAGE_SIZE); }
        }, DOWN {
            int align(int v, int base) { return v & ~(base-1); }
            long align(long v, long base) { return v & ~(base-1); }
            public int align4(int v) { return align(v, 4); }
            public int align8(int v) { return align(v, 8); }
            public int alignPage(int v) { return align(v, PAGE_SIZE); }
            public long alignPage(long v) { return align(v, PAGE_SIZE); }
        };

        final static int PAGE_SIZE = Unsafe.UNSAFE.pageSize();
        static {
            if (PAGE_SIZE % 4 != 0) throw new Error();
        }

        public abstract int align4(int v);
        public abstract int align8(int v);
        public abstract int alignPage(int v);
        public abstract long alignPage(long v);
    }


    /**
     * FieldUpdater for BitMap implemented by int/long
     * */
    public static class BitsHolder {
        private final long offset;
        private final boolean int32;

        public BitsHolder(Class<?> clazz, String field) {
            try {
                Field f = clazz.getDeclaredField(field);
                if (f.getType() != int.class && f.getType() != long.class) {
                    throw new IllegalArgumentException(field);
                }
                int32 = f.getType() == int.class;
                offset = UNSAFE.objectFieldOffset(f);
            } catch (Exception e) { throw new Error(e);  }
        }

        private boolean cas(Object obj, long expect, long update) {
            if (int32) {
                return UNSAFE.compareAndSwapInt(obj, offset, (int)expect, (int)update);
            } else {
                return UNSAFE.compareAndSwapLong(obj, offset, expect, update);
            }
        }

        public boolean cas0(Object obj, long expect, int bit) {
            if (bit < 0 || bit > 63 || (int32 && bit>31))
                throw new Error();
            if (((expect >>> bit) & 1) == 0) return true;
            return cas(obj, expect, expect & ~(1L << bit));
        }

        public boolean cas1(Object obj, long expect, int bit) {
            if (bit < 0 || bit > 63 || (int32 && bit>31))
                throw new Error();
            if (((expect >>> bit) & 1) == 1) return true;
            return cas(obj, expect, expect | (1L << bit));
        }

        public int bits() { return int32? 32: 64; }
    }

    /**
     * Accessor for raw memory
     * */
    public static abstract class Accessor {

        static final int
                INT_ARR_OFFSET = UNSAFE.arrayBaseOffset(int[].class),
                BYTE_ARR_OFFSET = UNSAFE.arrayBaseOffset(byte[].class),
                LONG_ARR_OFFSET = UNSAFE.arrayBaseOffset(long[].class);

        static void transfer(Object src, long src0, Object dst, long dst0, long len) {
            UNSAFE.copyMemory(src, src0, dst, dst0, len);
        }

        protected abstract long address();

        protected abstract long length();

        private void check(long offset, int length) {
            if (offset < 0 || length < 0 || offset+length > length()) throw new IndexOutOfBoundsException();
        }

        private long putArray(long ptr, long pos, Object arr, int abo, int beg, int len, int shl) {
            beg <<= shl; len <<= shl;
            check(pos, len);
            transfer(arr, abo + beg, null, ptr + pos, len);
            return pos + len;
        }

        private long getArray(long ptr, long pos, Object arr, int abo, int beg, int len, int shl) {
            beg <<= shl; len <<= shl;
            check(pos, len);
            transfer(null, ptr + pos, arr, abo + beg, len);
            return pos + len;
        }

        public void putInt(long pos, int i) {
            check(pos, 4); UNSAFE.putInt(address() + pos, i);
        }

        public int getInt(long pos) {
            check(pos, 4); return UNSAFE.getInt(address() + pos);
        }

        public void putByte(long pos, byte b) {
            check(pos, 1); UNSAFE.putByte(address() + pos, b);
        }

        public byte getByte(long pos) {
            check(pos, 1); return UNSAFE.getByte(address() + pos);
        }

        public void putLong(long pos, long l) {
            check(pos, 8); UNSAFE.putLong(address() + pos, l);
        }

        public long getLong(long pos) {
            check(pos, 8); return UNSAFE.getLong(address() + pos);
        }

        public long putInts(long pos, int[] arr, int beg, int len) {
            return putArray(address(), pos, arr, INT_ARR_OFFSET, beg, len, 2);
        }

        public long getInts(long pos, int[] arr, int beg, int len) {
            return getArray(address(), pos, arr, INT_ARR_OFFSET, beg, len, 2);
        }

        public long putBytes(long pos, byte[] arr, int beg, int len) {
            return putArray(address(), pos, arr, BYTE_ARR_OFFSET, beg, len, 0);
        }

        public long getBytes(long pos, byte[] arr, int beg, int len) {
            return getArray(address(), pos, arr, BYTE_ARR_OFFSET, beg, len, 0);
        }

        public long putLongs(long pos, byte[] arr, int beg, int len) {
            return putArray(address(), pos, arr, LONG_ARR_OFFSET, beg, len, 3);
        }

        public long getLongs(long pos, byte[] arr, int beg, int len) {
            return getArray(address(), pos, arr, LONG_ARR_OFFSET, beg, len, 3);
        }
    }

}
