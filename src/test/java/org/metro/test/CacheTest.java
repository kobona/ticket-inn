package org.metro.test;

import org.junit.Test;
import org.metro.cache.impl.CacheBuilder;
import org.metro.cache.impl.SelfEvictCache;
import org.metro.cache.serial.SpaceWrapper;

/**
 * <p> Created by pengshuolin on 2019/6/9
 */
public class CacheTest {

    @Test
    public void testBuild() {
        SelfEvictCache<String, SpaceWrapper> cache = new CacheBuilder("test:abc").build();
        cache.put("PING",  SpaceWrapper.put("PONG", cache));
        System.out.println(cache.get("PING").get());
    }
}
