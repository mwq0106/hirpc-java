package com.hirpc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author mwq0106
 * @date 2019/9/25
 */
@Configuration
@EnableConfigurationProperties(ApplicationConfig.class)
@ConfigurationProperties(prefix = "hirpc.application")
public class ApplicationConfig {
    private String version;
    private String name;


    public void setVersion(String version) {
        this.version = version;
    }

    public void setName(String name) {
        this.name = name;
    }



    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

}
