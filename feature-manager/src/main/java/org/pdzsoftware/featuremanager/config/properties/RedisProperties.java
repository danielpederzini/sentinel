package org.pdzsoftware.featuremanager.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "feature.cache.redis")
public class RedisProperties {
    private Duration timeToLast = Duration.ofMinutes(10);
}

