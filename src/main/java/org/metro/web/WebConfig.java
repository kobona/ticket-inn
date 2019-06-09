package org.metro.web;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p> Created by pengshuolin on 2019/6/4
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] YAML_CONFIG_FILES = {};

    @Bean
    public static PropertySourcesPlaceholderConfigurer yamlPropertiesConfigurer() {
        ClassPathResource[] resources = Arrays.stream(YAML_CONFIG_FILES).map(ClassPathResource::new).toArray(ClassPathResource[]::new);
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(resources);
        propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
        return propertySourcesPlaceholderConfigurer;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter strConverter = new StringHttpMessageConverter();
        strConverter.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(strConverter);

        ByteArrayHttpMessageConverter bytesConverter = new ByteArrayHttpMessageConverter();
        converters.add(bytesConverter);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(builder.build());
        jsonConverter.setPrettyPrint(false);
        jsonConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
        converters.add(jsonConverter);
    }

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }

}
