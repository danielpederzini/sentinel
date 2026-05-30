package org.pdzsoftware.riskactionhandler.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.pdzsoftware.riskactionhandler.infrastructure.config.properties.KafkaConsumerProperties;
import org.pdzsoftware.riskactionhandler.infrastructure.inbound.consumer.dto.TransactionScoredMessage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(KafkaConsumerProperties.class)
public class KafkaConsumerConfig {
	@Bean
	public ConsumerFactory<String, TransactionScoredMessage> transactionConsumerFactory(KafkaConsumerProperties kafkaConsumerProperties) {
		Map<String, Object> consumerConfigs = new HashMap<>();
		consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerProperties.getBootstrapServers());
		consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerProperties.getConsumerGroupId());
		consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerProperties.getAutoOffsetReset());
		consumerConfigs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		JacksonJsonDeserializer<TransactionScoredMessage> valueDeserializer = new JacksonJsonDeserializer<>(TransactionScoredMessage.class);
		valueDeserializer.setUseTypeHeaders(false);

		ErrorHandlingDeserializer<TransactionScoredMessage> errorHandlingDeserializer =
				new ErrorHandlingDeserializer<>(valueDeserializer);

		return new DefaultKafkaConsumerFactory<>(
				consumerConfigs,
				new StringDeserializer(),
				errorHandlingDeserializer
		);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, TransactionScoredMessage> kafkaListenerContainerFactory(
			ConsumerFactory<String, TransactionScoredMessage> transactionConsumerFactory,
			DefaultErrorHandler kafkaErrorHandler) {
		ConcurrentKafkaListenerContainerFactory<String, TransactionScoredMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(transactionConsumerFactory);
		factory.setCommonErrorHandler(kafkaErrorHandler);
		return factory;
	}

	@Bean
	public ProducerFactory<String, Object> deadLetterProducerFactory(KafkaConsumerProperties kafkaConsumerProperties) {
		Map<String, Object> producerConfigs = new HashMap<>();
		producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerProperties.getBootstrapServers());

		DefaultKafkaProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs);
		producerFactory.setKeySerializer(new StringSerializer());

		Map<Class<?>, org.apache.kafka.common.serialization.Serializer<?>> delegates = new LinkedHashMap<>();
		delegates.put(byte[].class, new ByteArraySerializer());
		delegates.put(Object.class, new JacksonJsonSerializer<>());
		producerFactory.setValueSerializer(new DelegatingByTypeSerializer(delegates, true));

		return producerFactory;
	}

	@Bean
	public KafkaTemplate<String, Object> deadLetterKafkaTemplate(ProducerFactory<String, Object> deadLetterProducerFactory) {
		return new KafkaTemplate<>(deadLetterProducerFactory);
	}

	@Bean
	public DefaultErrorHandler kafkaErrorHandler(
			KafkaTemplate<String, Object> deadLetterKafkaTemplate,
			KafkaConsumerProperties kafkaConsumerProperties,
			MeterRegistry meterRegistry) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate);
		ConsumerRecordRecoverer countingRecoverer = (record, exception) -> {
			meterRegistry.counter("kafka_dead_letter", "topic", record.topic()).increment();
			recoverer.accept(record, exception);
		};

		KafkaConsumerProperties.Retry retry = kafkaConsumerProperties.getRetry();
		ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(retry.getMaxAttempts());
		backOff.setInitialInterval(retry.getInitialIntervalMs());
		backOff.setMultiplier(retry.getMultiplier());
		backOff.setMaxInterval(retry.getMaxIntervalMs());

		DefaultErrorHandler errorHandler = new DefaultErrorHandler(countingRecoverer, backOff);
		errorHandler.addNotRetryableExceptions(
				MethodArgumentNotValidException.class,
				ConstraintViolationException.class);
		return errorHandler;
	}
}
