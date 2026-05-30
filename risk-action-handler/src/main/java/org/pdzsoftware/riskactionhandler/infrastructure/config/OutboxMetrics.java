package org.pdzsoftware.riskactionhandler.infrastructure.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.pdzsoftware.riskactionhandler.domain.enums.NotificationTaskStatus;
import org.pdzsoftware.riskactionhandler.infrastructure.outbound.repository.NotificationTaskRepository;
import org.springframework.stereotype.Component;

/**
 * Registers a gauge per notification-outbox status so the dashboard can show how many
 * tasks are pending, completed, or dead-lettered at any moment.
 */
@Component
public class OutboxMetrics {

	public OutboxMetrics(MeterRegistry meterRegistry, NotificationTaskRepository repository) {
		for (NotificationTaskStatus status : NotificationTaskStatus.values()) {
			Gauge.builder("notification_outbox_tasks", repository, repo -> repo.countByStatus(status))
					.description("Number of notification-outbox tasks in each status")
					.tag("status", status.name())
					.register(meterRegistry);
		}
	}
}
