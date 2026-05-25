package org.pdzsoftware.riskactionhandler.infrastructure.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.notification.scheduler")
public class NotificationSchedulerProperties {
    private int maxAttempts = 5;
    private long baseDelaySeconds = 30;
    private int batchSize = 50;
}
