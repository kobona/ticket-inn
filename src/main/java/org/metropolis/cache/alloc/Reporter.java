package org.metropolis.cache.alloc;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Status of Allocator
 * */
public class Reporter {

    private final static HashMap<String,Reporter> instances = new HashMap<>();
    private static synchronized boolean checkName(String name, Reporter reporter) {
        if (!instances.containsKey(name)) {
            instances.put(name, reporter);
            return true;
        }
        return false;
    }

    public class Detail {
        public final long from, to, cnt, num, total;
        Detail(long from, long to, long cnt, long num, long total) {
            this.from = from; this.to = to; this.cnt = cnt; this.num = num; this.total = total;
        }
    }

    public class Brief {

        private String reverseBinary(long bits, int len) {
            String s = Long.toBinaryString(Long.reverse(bits) >>> (64-len));
            if (s.length() < len) {
                s = String.join("", Collections.nCopies(len - s.length(), "0")) + s;
            }
            return s;
        }

        public final String
            smallMap = reverseBinary(allocator.smallBin.map, allocator.smallBin.bits()),
            treeMap = reverseBinary(allocator.treeBin.map, allocator.treeBin.bits());

        public final long
            okSize = Reporter.this.okSize.sum(),
            freeSize = Reporter.this.freeSize.sum(),
            leakSize = Reporter.this.leakSize.sum();

        public final int
            okNum = Reporter.this.okNum.intValue(),
            freeNum = Reporter.this.freeNum.intValue(),
            leakNum = Reporter.this.leakNum.intValue(),
            failNum = Reporter.this.failNum.intValue(),
            collNum = Reporter.this.collNum.intValue(),
            active = allocator.active(),
            blocked = allocator.blocked();

        public final Detail[] details;
        public final long total, available, smallSize, treeSize, smallNum, treeNum, treeCnt;
        Brief(long total, long available,
              long smallSize, long treeSize,
              long smallNum, long treeNum, long treeCnt,
              Detail[] details) {
            this.total = total;
            this.available = available;
            this.details = details;
            this.smallSize = smallSize;
            this.treeSize = treeSize;
            this.smallNum = smallNum;
            this.treeNum = treeNum;
            this.treeCnt = treeCnt;
        }

        public String toString() {
            return print(this);
        }
    }

    private final LongAdder
            okSize = new LongAdder(),
            okNum = new LongAdder(),
            freeSize = new LongAdder(),
            freeNum = new LongAdder(),
            leakSize = new LongAdder(),
            leakNum = new LongAdder(),
            failNum = new LongAdder(),
            collNum = new LongAdder();

    private final Allocator allocator;

    Reporter(String name, Allocator allocator) {
        checkName(name, this);
        this.allocator = allocator;
    }

    void alloc(long size, boolean ok) {
        if (ok) {
            okSize.add(size);
            okNum.increment();
        } else {
            failNum.increment();
        }
    }

    void free(long size) {
        freeSize.add(size);
        freeNum.increment();
    }

    void leak(long size) {
        leakSize.add(size);
        leakNum.increment();
    }

    void collFrag() {
        collNum.increment();
    }

    public Brief info(boolean detail) {

        long available = -1, smallSize = -1, treeSize = -1, smallNum = -1, treeNum = -1, treeCnt = -1;
        Detail[] details = null;

        if (detail) {
            int[] small = allocator.smallBin.stats();
            long[] tree = allocator.treeBin.stats();

            int n = 0;
            int length = small.length/2 + tree.length/4;
            details = new Detail[length + 1];

            details[n++] = new Detail(0, 0, 1, 1, allocator.lastRemain.size());

            for (int i=0; i<small.length; i+=2) {
                int size = small[i], num = small[i+1];
                details[n++] = new Detail(size, size, 1, num, size*num);
            }

            for (int i=0; i<tree.length; i+=4) {
                long from = tree[i], to = (i+4<tree.length)? tree[i+4]: -1;
                details[n++] = new Detail(from, to, tree[i+1], tree[i+2], tree[i+3]);
            }

            available = 0;
            smallSize = treeSize = smallNum = treeNum = treeCnt = 0;
            for (Detail d: details) {
                available += d.total;
                if (d.from == d.to)  {
                    smallSize += d.total;
                    smallNum += d.num;
                } else  {
                    treeSize += d.total;
                    treeNum += d.num;
                    treeCnt += d.cnt;
                }
            }
        }

        return new Brief(allocator.size(), available,
                smallSize, treeSize, smallNum, treeNum, treeCnt, details);
    }

    private String print(Brief info) {
        StringBuilder sb = new StringBuilder();
        sb.append("active:\t").append(info.active).append('\n');
        sb.append("blocked:\t").append(info.blocked).append('\n');
        sb.append("total:\t").append(info.total).append('\n');
        sb.append("available:\t").append(info.available).append('\n');
        sb.append("smallSize:\t").append(info.smallSize).append('\n');
        sb.append("treeSize:\t").append(info.treeSize).append('\n');
        sb.append("smallNum:\t").append(info.smallNum).append('\n');
        sb.append("treeNum:\t").append(info.treeNum).append('\n');
        sb.append("treeCnt:\t").append(info.treeCnt).append('\n');
        sb.append("smallMap:\t").append(info.smallMap).append('\n');
        sb.append("treeMap:\t").append(info.treeMap).append('\n');
        sb.append("okNum:\t").append(info.okNum).append('\n');
        sb.append("okSize:\t").append(info.okSize).append('\n');
        sb.append("freeNum:\t").append(info.freeNum).append('\n');
        sb.append("freeSize:\t").append(info.freeSize).append('\n');
        sb.append("leakNum:\t").append(info.leakNum).append('\n');
        sb.append("leakSize:\t").append(info.leakSize).append('\n');
        sb.append("failNum:\t").append(info.failNum).append('\n');
        sb.append("collNum:\t").append(info.collNum).append('\n');

        if (info.details != null) {
            for (Detail d: info.details) {
                sb.append(String.format("%d %s %s %d %d\n",
                        d.from, (d.from == d.to || d.to < 0? "*": ""+d.to),
                        (d.from == d.to ? "*": ""+d.cnt), d.num, d.total));
            }
        }

        return sb.toString();
    }
}
