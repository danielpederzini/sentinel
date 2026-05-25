package org.pdzsoftware.riskactionhandler.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdzsoftware.riskactionhandler.application.usecase.NotifyRiskUseCase;
import org.pdzsoftware.riskactionhandler.domain.entity.NotificationTask;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.NotificationSchedulerProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository.NotificationTaskRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(NotificationSchedulerProperties.class)
public class NotificationTaskScheduler {
    private static final List<NotificationTaskStatus> RETRYABLE_STATUSES = List.of(
            NotificationTaskStatus.PENDING_LLM,
            NotificationTaskStatus.PENDING_EMAIL
    );

    private final NotificationTaskRepository notificationTaskRepository;
    private final NotifyRiskUseCase notifyRiskUseCase;
    private final NotificationSchedulerProperties schedulerProperties;

    @Scheduled(fixedDelayString = "${app.notification.scheduler.poll-interval-ms:30000}")
    public void processNotificationTasks() {
        List<NotificationTask> tasks = notificationTaskRepository
                .findByStatusInAndNextRetryAtLessThanEqual(RETRYABLE_STATUSES, Instant.now());

        if (tasks.isEmpty()) {
            return;
        }

        log.info("Processing {} pending notification tasks", tasks.size());

        int batchSize = schedulerProperties.getBatchSize();
        tasks.stream()
                .limit(batchSize)
                .forEach(notifyRiskUseCase::execute);
    }
}
