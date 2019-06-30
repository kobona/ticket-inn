package org.metropolis.test;

import org.junit.Assert;
import org.junit.Test;
import org.metropolis.cache.alloc.Reporter;
import org.metropolis.cache.impl.CacheBuilder;
import org.metropolis.cache.impl.CacheEngine;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

/**
 * <p> Created by pengshuolin on 2019/6/9
 */
public class CacheTest {

    @Test
    public void testBuild() {
        CacheEngine<String, String> cache = new CacheBuilder("test:newInstance").build();
        cache.put("PING",  "PONG", false);
        System.out.println(cache.getIfPresent("PING").get());
    }

    @Test
    public void testBasic() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        CacheEngine<Integer, Integer> cache = new CacheBuilder("test:basic").concurrencyLevel(10)
                .build();

        for (int i=0; i<100000; i++) {
            cache.put(i, i, false);
        }
        Assert.assertEquals(cache.size(), 100000);
        Assert.assertEquals(cache.keySet().size(), 100000);

        int j = 0;
        for (Integer k : cache.keySet()) {
            j++;
            Assert.assertTrue(k >= 0 && k<100000);
        }
        Assert.assertEquals(100000, j);

        j = 0;
        for (Integer v : cache.values()) {
            j++;
            Assert.assertTrue(v >= 0 && v<100000);
        }
        Assert.assertEquals(100000, j);

        j = 0;
        for (Map.Entry<Integer, Integer> e : cache.entries()) {
            j++;
            Assert.assertEquals(e.getKey(), e.getValue());
            Assert.assertTrue(e.getKey() >= 0 && e.getKey() <100000);
        }
        Assert.assertEquals(100000, j);

        for (int i=0; i<100000; i++) {
            int n = random.nextInt(10000000);
            cache.put(n, n, false);
        }

        cache.clear();
        Assert.assertEquals(0, cache.size());
        System.out.println(cache.stats());
        Reporter.Brief brief = cache.mem().reporter().info(true);
        System.out.println(brief);
        Assert.assertEquals(brief.total, brief.available);
    }

    @Test
    public void testThreadSafe() throws InterruptedException {

        final int num = 45;

        final CyclicBarrier start = new CyclicBarrier(num);
        final CountDownLatch end = new CountDownLatch(num);

        CacheEngine<Integer, String> cache =
                new CacheBuilder("test:thread.safe")
                        .virtualSpace("1.5GB")
                        .maximumSpace("1GB")
                        .concurrencyLevel(30)
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
                    for (int i=0; i<100000; i++) {
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
        System.out.println(cache.stats());

        Reporter.Brief brief = cache.mem().reporter().info(true);
        System.out.println(brief);
        Assert.assertEquals(brief.total, brief.available);
        Assert.assertEquals(brief.active, 0);
        Assert.assertEquals(brief.blocked, 0);
    }

}
