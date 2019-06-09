package org.metro.test;

import org.junit.Assert;
import org.junit.Test;
import org.metro.cache.alloc.Reporter;
import org.metro.cache.impl.CacheBuilder;
import org.metro.cache.impl.SelfEvictCache;
import org.metro.cache.serial.SpaceWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * <p> Created by pengshuolin on 2019/6/9
 */
public class CacheTest {

    @Test
    public void testBuild() {
        SelfEvictCache<String, SpaceWrapper> cache = new CacheBuilder("test:build").build();
        cache.put("PING",  SpaceWrapper.put("PONG", cache));
        System.out.println(cache.get("PING").get());
    }

    @Test
    public void testThreadSafe() {

        final int num = 45;

        final CyclicBarrier start = new CyclicBarrier(num);
        final CountDownLatch end = new CountDownLatch(num);

        SelfEvictCache<Integer, SpaceWrapper> cache = new CacheBuilder("test:thread.safe").build();

        for (int n=0; n<num; n++) {

            final String name = "thread-" + n;

            new Thread(()->{

                final String uuid = UUID.randomUUID().toString();

                try { start.await(); }
                catch (Exception e) {
                    e.printStackTrace();
                }

                Random random = new Random();
                for (int i=0; i<100000; i++) {
                    int k = random.nextInt(500);
                    if (random.nextInt(5) != 0) {
                        cache.put(k,  SpaceWrapper.put(uuid+k, cache));
                    } else {
                        cache.remove(k);
                    }
                }

                System.out.println(name + ": mission complete");
                end.countDown();

            }).start();
        }

        try {
            end.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        cache.clear();

        Reporter.Brief brief = cache.mem().reporter().info(true);
        System.out.println(brief);
        Assert.assertEquals(brief.total, brief.available);
        Assert.assertEquals(brief.active, 0);
        Assert.assertEquals(brief.blocked, 0);
    }

}
