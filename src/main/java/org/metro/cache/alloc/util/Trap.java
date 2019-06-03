package org.metro.cache.alloc.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exclusive Global Lock
 */
public class Trap {

    private final static int SPIN_LIMIT = 30;
    private final static int NULL = -1;

    private volatile long lastTime = System.currentTimeMillis();

    private volatile int blocked = 0;
    private volatile boolean enable = false;
    private final AtomicInteger active = new AtomicInteger(0);

    private final Object lock = new Object();
    private final Runnable onSafe;
    private final long minInterval;

    public Trap(Runnable exclusiveTask, long taskInterval) {
        onSafe = exclusiveTask;
        minInterval = taskInterval;
    }

    public boolean trigger() {

        long now = System.currentTimeMillis();

        if (! executable(now)) return false;

        synchronized (lock) {
            if (!enable) {
                enable = true;

                // if (active.get() == 0) {
                if (active.compareAndSet(0, NULL)) {
                    execute(now);
                    return true;
                }
            }
            return false;
        }
    }

    private void reset() {
        synchronized (lock) {
            if (enable) {
                enable = false;
                lock.notifyAll();
            }
        }
    }

    public void enter() {
        while (true) {
            while (enable) {
                synchronized (lock){
                    blocked++;
                    while (enable) try { lock.wait(); }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    blocked--;
                }
            }

            int count = SPIN_LIMIT;
            while (count-- > 0 && active.updateAndGet(i -> i == NULL ? NULL: i+1) == NULL);
            if (count > 0) break;
        }
    }

    public void quit() {
        int state = active.decrementAndGet();
        if (state < 0)
            throw new AssertionError();
        if (state == 0 && enable) {
            long now = System.currentTimeMillis();
            synchronized (lock) {
                if (enable && active.compareAndSet(0, NULL)) {
                    execute(now);
                }
            }
        }
    }

    private boolean executable(long now) {
        return now - lastTime >= minInterval;
    }

    /** use synchronization block to ensure absolute safety */
    private void execute(long now) {

        if (!enable)
            throw new IllegalStateException("execute: enable = false");

        if (executable(now)) {
            lastTime = now;

            try {
                onSafe.run();
            } catch (Throwable t) {
                throw t;
            }
        }

        if (!active.compareAndSet(NULL, 0))
            throw new AssertionError();

        reset();
    }

    public int active() { return active.get(); }

    public int blocked() { return blocked; }

    public long lastTime() { return lastTime; }
}
