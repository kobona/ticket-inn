package org.metropolis.cache.alloc;

import org.metropolis.cache.alloc.util.Stack;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MemoryLeak Detector (not strictly tested yet)
 * */
public class Detector {

    static final class Ref extends WeakReference<Allocator.Space> {
        long address;
        final int size;
        public Ref(Allocator.Space ref, long address, int size, ReferenceQueue<? super Allocator.Space> queue) {
            super(ref, queue);
            this.address = address;
            this.size = size;
        }
        public boolean free() {
            return address == Stack.NULL;
        }
    }

    static final CopyOnWriteArrayList<Detector> instances = new CopyOnWriteArrayList<>();
    static final Timer timer = new Timer("LeakDetector", true);
    static {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
            for (Detector inst: instances) {
                for (Ref ref; (ref = (Ref)inst.refQueue.poll()) != null;) {
                    if (! ref.free()) {
                        inst.allocator.free(ref);
                        inst.allocator.report().leak(ref.size);
                    }
                }
            }
            }
        }, 1000, 1000);
    }


    private final Allocator allocator;
    private final ReferenceQueue<Allocator.Space> refQueue = new ReferenceQueue<>();

    public Detector(Allocator allocator) {
        this.allocator = allocator;
        instances.add(this);
    }

    public Ref attach(Allocator.Space space, long address, int size) {
        return new Ref(space, address, size, refQueue);
    }

}
