package org.metro.cache;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.StringUtils;
import org.metro.cache.impl.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p> Created by pengshuolin on 2019/6/5
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private final static Logger log = LoggerFactory.getLogger(ShiroCacheManager.class);

    static CacheBuilder parseJsonConfig(String json) {
        try {
            String name = null;
            String strategy = "FIFO";
            String virtualSpace = null;
            String maximumSpace = null;
            Integer maximumSize = null;
            Integer expiryAfterWrite = null;
            Integer expiryAfterAccess = null;

            JsonParser parser = new JsonFactory().createParser(json);
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                switch (StringUtils.defaultString(parser.currentName())) {
                    case "name": name = parser.getValueAsString(); break;
                    case "strategy": strategy = parser.getValueAsString("FIFO"); break;
                    case "virtualSpace": virtualSpace = parser.getValueAsString(); break;
                    case "maximumSpace": maximumSpace = parser.getValueAsString(); break;
                    case "maximumSize": maximumSize = parser.getValueAsInt(0); break;
                    case "expiryAfterWrite": expiryAfterWrite = parser.getValueAsInt(0); break;
                    case "expiryAfterAccess": expiryAfterAccess = parser.getValueAsInt(0); break;
                }
            }

            CacheBuilder builder = new CacheBuilder(name);
            switch (strategy.toUpperCase()) {
                case "FIFO": builder.applyFIFO(); break;
                case "LRU": builder.applyLRU(); break;
                case "LFU": builder.applyLFU(); break;
                default: throw new IllegalArgumentException(strategy);
            }
            if (virtualSpace != null)
                builder.virtualSpace(virtualSpace);
            if (maximumSpace != null)
                builder.maximumSpace(maximumSpace);
            if (maximumSize != null)
                builder.maximumSize(maximumSize);
            if (expiryAfterWrite != null)
                builder.expiryAfterWrite(expiryAfterWrite);
            if (expiryAfterAccess != null)
                builder.expiryAfterAccess(expiryAfterAccess);

            return builder;
        } catch (Exception e) {
            log.error("parse cache config fail {}", json, e);
            throw new RuntimeException(e);
        }
    }

    @Value("${cache.prefix-dir:}")
    private String prefixDir;

    @PostConstruct
    public void prepareCacheDir() throws IOException {
        Files.createDirectories(Paths.get(prefixDir));
    }

    @Bean
    public CacheManager cacheManager() {
        return new SpringCacheManager(prefixDir);
    }

    @Bean
    public ShiroCacheManager shiroCacheManager() {
        return new ShiroCacheManager(prefixDir);
    }

}
