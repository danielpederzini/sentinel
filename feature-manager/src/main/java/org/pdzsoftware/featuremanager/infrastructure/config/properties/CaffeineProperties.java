package org.pdzsoftware.featuremanager.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
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
            "userAverageAmounts",
            "ipRiskScores"
    );

    private long maximumSize = 1000;
    private Duration expireAfterWrite = Duration.ofMinutes(10);
}

