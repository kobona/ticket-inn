package org.metro.cache.alloc.util;

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

