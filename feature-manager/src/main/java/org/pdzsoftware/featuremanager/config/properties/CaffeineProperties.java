package org.pdzsoftware.featuremanager.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "feature.cache.caffeine")
public class CaffeineProperties {
    private List<String> cacheNames = List.of(
            "merchants",
            "cards",
            "users",
            "trustedDevices",
            "merchantsExists",
            "cardsExists",
            "usersExists",
            "trustedDevicesExists",
            "userAverageAmounts"
    );

    private long maximumSize = 1000;
    private Duration expireAfterWrite = Duration.ofMinutes(10);
}

