package org.metro.test;

import org.junit.Assert;
import org.junit.Test;
import org.metro.cache.alloc.Reporter;
import org.metro.cache.impl.CacheBuilder;
import org.metro.cache.impl.CacheTemplate;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * <p> Created by pengshuolin on 2019/6/9
 */
public class CacheTest {

    @Test
    public void testBuild() {
        CacheTemplate<String, String> cache = new CacheBuilder("test:newInstance").build();
        cache.put("PING",  "PONG", false);
        System.out.println(cache.getIfPresent("PING").get());
    }

    @Test
    public void testThreadSafe() throws InterruptedException {

        final int num = 45;

        final CyclicBarrier start = new CyclicBarrier(num);
        final CountDownLatch end = new CountDownLatch(num);

        CacheTemplate<Integer, String> cache =
                new CacheBuilder("test:thread.safe")
                        .virtualSpace("1GB")
                        .expiryAfterWrite(100)
                        .applyFIFO().build();

        for (int n=0; n<num; n++) {

            final String name = "thread-" + n;

            new Thread(()->{

                final String uuid = UUID.randomUUID().toString();

                try { start.await(); }
                catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Random random = new Random();
                    for (int i=0; i<1000000; i++) {
                        int k = random.nextInt(500);
                        switch (random.nextInt(3)) {
                            case 0: cache.put(k, String.join("", Collections.nCopies(k, uuid))+k, false); break;
//                            case 0: cache.put(k,  SpaceWrapper.put(uuid+k, cache)); break;
                            case 1: cache.getIfPresent(k); break;
                            case 2: cache.remove(k, false); break;
                        }
                        if (random.nextInt(10000) == 0) {
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    System.out.println(name + ": mission complete");
                } finally {
                    end.countDown();
                }

            }).start();
        }

        try {
            end.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cache.clear();
        Thread.sleep(5000);
        cache.clear();

        System.out.println(cache.stats());

        Reporter.Brief brief = cache.mem().reporter().info(true);
        System.out.println(brief);
        Assert.assertEquals(brief.total, brief.available);
        Assert.assertEquals(brief.active, 0);
        Assert.assertEquals(brief.blocked, 0);
    }

}
