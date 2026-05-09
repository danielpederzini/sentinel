package org.pdzsoftware.featuremanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "feature.cache")
public class CacheProperties {
    private long secondsToLast;
}

